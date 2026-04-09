package ru.practicum.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.stereotype.Component;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.aggregator.config.ActionWeightsConfig;
import ru.practicum.stats.aggregator.config.KafkaConfig;
import serialization.avro.GeneralAvroSerializer;
import serialization.avro.UserActionDeserializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class Aggregator {
    private final KafkaConfig kafkaConfig;

    private final Map<ActionTypeAvro, Double> actionWeights;
    // eventId, <userId, maxWeight>
    private final Map<Long, Map<Long, Double>> userMaxWeightsMatrix = new HashMap<>();
    // числитель - eventId, <eventId, sumOfMinWeights>
    private final Map<Long, Map<Long, Double>> sumOfMinWeightsMatrix = new HashMap<>();
    // знаменатель - eventId, sumOfUserWeights
    private final Map<Long, Double> sumOfUserWeightsMap = new HashMap<>();
    private final Set<Long> events = new HashSet<>();

    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer; // TODO: key
    // TODO: offset commits
    //private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    @Autowired
    public Aggregator(KafkaConfig kafkaConfig, ActionWeightsConfig weightsConfig) {
        this.kafkaConfig = kafkaConfig;

        actionWeights = new HashMap<>();
        actionWeights.put(ActionTypeAvro.ACTION_VIEW, weightsConfig.getView());
        actionWeights.put(ActionTypeAvro.ACTION_REGISTER, weightsConfig.getRegister());
        actionWeights.put(ActionTypeAvro.ACTION_LIKE, weightsConfig.getLike());

        Properties consumerConfig = new Properties();
        Properties producerConfig = new Properties();

        // TODO: methods
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConfig.getConsumer().getClientId());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getConsumer().getGroupId());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getServer());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        //consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getServer());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);

        consumer = new KafkaConsumer<>(consumerConfig);
        producer = new KafkaProducer<>(producerConfig);

        log.info("Aggregator is using Kafka-server at url: {}", kafkaConfig.getServer());

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    public void start() {
        try {
            String actionsTopic = kafkaConfig.getTopics().getUsers();
            consumer.subscribe(List.of(actionsTopic));
            log.info("Aggregator subscribed to the topic: {}", actionsTopic);

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(Duration.ofMillis(kafkaConfig.getConsumer().getPollDurationMs()));

                List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

                for (ConsumerRecord<Long, UserActionAvro> consumerRecord : records) {
                    UserActionAvro userAction = consumerRecord.value();
                    log.debug("Received user action: {}", userAction);

                    List<EventSimilarityAvro> similarities = calculateSimilarities(userAction);
                    eventSimilarities.addAll(similarities);
                }

                if (!eventSimilarities.isEmpty()) {
                    log.debug("Sending similarity scores to the topic [{}]", kafkaConfig.getTopics().getEvents());

                    for (EventSimilarityAvro eventSimilarityAvro : eventSimilarities) {
                        log.debug("Event similarity: {}", eventSimilarityAvro);
                        ProducerRecord<String, SpecificRecordBase> producerRecord =
                                new ProducerRecord<>(kafkaConfig.getTopics().getEvents(), null, eventSimilarityAvro);
                        producer.send(producerRecord);
                    }
                }

                log.debug("User actions have been processed");
            }
        } catch (WakeupException ignored) {

        } catch (Exception exception) {
            log.error("Error processing user action: {}", exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
        } finally {
            try {
                producer.flush();
                //consumer.commitSync(currentOffsets);
            } finally {
                log.info("Closing Aggregator Kafka-producer...");
                producer.close();

                log.info("Closing Aggregator Kafka-consumer...");
                consumer.close();
            }
        }
    }

    private List<EventSimilarityAvro> calculateSimilarities(UserActionAvro userAction) {
        Long user = userAction.getUserId();
        Long event = userAction.getEventId();
        Double weight = actionWeights.get(userAction.getActionType());

        // вектор весов пользователей для мероприятия
        Map<Long, Double> userMaxWeightsVector = userMaxWeightsMatrix.get(event);

        if (userMaxWeightsVector == null) {
            return handleNewEvent(event, user, weight);
        } else {
            return handleExistingEvent(event, userMaxWeightsVector, user, weight);
        }
    }

    private List<EventSimilarityAvro> handleNewEvent(Long event, Long user, Double weight) {
        List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

        Map<Long, Double> userMaxWeightsVector;
        log.debug("There are no user interactions with event ({})", event);
        log.debug("User max weights vector for event ({}) was null", event);
        log.debug("Creating new vector...");

        userMaxWeightsVector = new HashMap<>();
        userMaxWeightsVector.put(user, weight);
        log.debug("New user max weights vector for event ({}): {}", event, userMaxWeightsVector);
        userMaxWeightsMatrix.put(event, userMaxWeightsVector);

        events.add(event);

        // сразу определим знаменатель
        log.debug("As it's new event, sum of user weights equals weight ({})", weight);
        log.debug("Sum of user weights for event ({}): {}", event, weight);
        sumOfUserWeightsMap.put(event, weight);

        // создаем новый пустой вектор в числителе и кладем в матрицу
        sumOfMinWeightsMatrix.put(event, new HashMap<>());
        log.debug("Created new empty vector for sum of min weights of event ({})", event);

        // проходимся по матрице числителя?
        for (Long otherEvent : events) {
            if (event.equals(otherEvent)) {
                continue;
            }

            log.debug("Calculating similarity for other event ({})", otherEvent);

            Map<Long, Double> otherEventUserMaxWeightsVector = userMaxWeightsMatrix.get(otherEvent);
            log.debug("Other event ({}) user max weights vector: {}", otherEvent, otherEventUserMaxWeightsVector);

            HashSet<Long> userSet = new HashSet<>();
            userSet.add(user);
            userSet.addAll(otherEventUserMaxWeightsVector.keySet());
            log.debug("Users set: {}", userSet);

            double nominator = 0.0;

            for (var us : userSet) {
                nominator += Math.min(userMaxWeightsVector.getOrDefault(us, 0.0),
                        otherEventUserMaxWeightsVector.getOrDefault(us, 0.0));
            }

            log.debug("(nominator) Sum of min weights with other event ({}): {}", otherEvent, nominator);
            put(event, otherEvent, nominator);

            log.debug("(denominator part) Sum of weights for event ({}): {}", event, weight);

            Double otherEventSumOfWeights = sumOfUserWeightsMap.getOrDefault(otherEvent, 0.0);
            log.debug("(denominator part) Sum of weights for other event ({}): {}", otherEvent, otherEventSumOfWeights);

            if (otherEventSumOfWeights.equals(0.0)) {
                log.debug("Can not calculate similarity");
                continue;
            }

            Double denominator = Math.sqrt(weight) * Math.sqrt(sumOfUserWeightsMap.get(otherEvent));

            Double similarity = nominator / denominator;

            log.debug("Similarity score between event ({}) and other event ({}): {}", event, otherEvent, similarity);

            if (!similarity.equals(0.0)) {
                EventSimilarityAvro eventSimilarityAvro = getEventSimilarityAvro(event, otherEvent, similarity);

                eventSimilarities.add(eventSimilarityAvro);
            } else {
                log.debug("Similarity score was zero");
            }
        }

        return eventSimilarities;
    }

    private List<EventSimilarityAvro> handleExistingEvent(Long event, Map<Long, Double> userMaxWeightsVector, Long user, Double weight) {
        List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

        log.debug("There are existing user interactions with event ({})", event);
        Double oldWeight = userMaxWeightsVector.getOrDefault(user, 0.0);

        if (oldWeight.equals(0.0)) {
            log.debug("Old weight was 0");
        }

        // знаменатель
        Double oldSumOfUserWeights = sumOfUserWeightsMap.get(event);
        log.debug("Old sum of weights for event ({}): {}", event, oldSumOfUserWeights);
        Double delta = weight - oldWeight;

        Double newSumOfUserWeights = oldSumOfUserWeights;
        if (delta > 0) {
            newSumOfUserWeights += delta;
        }

        log.debug("New sum of weights for event ({}): {}", event, newSumOfUserWeights);
        sumOfUserWeightsMap.put(event, newSumOfUserWeights);

        if (weight <= oldWeight) {
            log.debug("Received weight equals old weight, no need to recalculate");
            return eventSimilarities;
        }

        log.debug("Received weight greater than old weight");
        userMaxWeightsVector.put(user, weight);

        log.debug("Recalculating similarity score");

        // проходимся по матрице числителя?
        for (Long otherEvent : events) {
            if (event.equals(otherEvent)) {
                continue;
            }

            log.debug("Calculating similarity for other event ({})", otherEvent);

            if (userMaxWeightsMatrix.get(otherEvent).get(user) == null) {
                log.debug("User ({}) didn't interact with other event ({}). No need to calculate", user, otherEvent);
                continue;
            }

            Double oldSumOfMinWeights = get(event, otherEvent);
            log.debug("(nominator) Old sum of min weights with other event ({}): {}", otherEvent, oldSumOfMinWeights);

            Double userWeightForAnotherEvent = userMaxWeightsMatrix.get(otherEvent).getOrDefault(user, 0.0);
            log.debug("Other event ({}) max user ({}) weight: {}", otherEvent, user, userWeightForAnotherEvent);

            Double oldMinWeight = Math.min(oldWeight, userWeightForAnotherEvent);
            Double newMinWeight = Math.min(weight, userWeightForAnotherEvent);
            Double deltainner = newMinWeight - oldMinWeight;
            log.debug("Delta: {}", deltainner);

            Double newSumOfMinWeights = oldSumOfMinWeights + deltainner;
            log.debug("(nominator) New sum of min weights with other event ({}): {}", otherEvent, newSumOfMinWeights);

            put(event, otherEvent, newSumOfMinWeights);

            // знаменатель
            log.debug("(denominator part) Sum of weights for event ({}): {}", event, newSumOfUserWeights);
            Double otherEventSumOfWeights = sumOfUserWeightsMap.getOrDefault(otherEvent, 0.0);
            log.debug("(denominator part) Sum of weights for other event ({}): {}", otherEvent, otherEventSumOfWeights);

            if (otherEventSumOfWeights.equals(0.0)) {
                log.debug("Can not calculate similarity");
                continue;
            }

            Double similarity = newSumOfMinWeights / (Math.sqrt(newSumOfUserWeights) * Math.sqrt(otherEventSumOfWeights));
            log.debug("Similarity score between event ({}) and other event ({}): {}", event, otherEvent, similarity);

            if (!similarity.equals(0.0)) {
                EventSimilarityAvro eventSimilarityAvro = getEventSimilarityAvro(event, otherEvent, similarity);

                eventSimilarities.add(eventSimilarityAvro);
            } else {
                log.debug("Similarity score was zero");
            }
        }

        return eventSimilarities;
    }

    private static EventSimilarityAvro getEventSimilarityAvro(Long event, Long otherEvent, Double similarity) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(event, otherEvent))
                .setEventB(Math.max(event, otherEvent))
                .setScore(similarity)
                .setTimestamp(Instant.now())
                .build();
    }

    private void put(long eventA, long eventB, double sum) {
        long first  = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        sumOfMinWeightsMatrix
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private double get(long eventA, long eventB) {
        long first  = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return sumOfMinWeightsMatrix
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }
}