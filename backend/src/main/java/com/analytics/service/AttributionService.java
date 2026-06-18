package com.analytics.service;

import com.analytics.dto.AttributionResult;
import com.analytics.dto.TouchPointDTO;
import com.analytics.entity.AttributionConfig;
import com.analytics.entity.AttributionModelType;
import com.analytics.repository.AttributionConfigRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AttributionService {
    private final AttributionConfigRepository attributionConfigRepository;

    public AttributionService(AttributionConfigRepository attributionConfigRepository) {
        this.attributionConfigRepository = attributionConfigRepository;
    }

    public AttributionResult calculate(List<String> channels, Long configId) {
        AttributionConfig config = resolveConfig(configId);
        AttributionModelType modelType = config.getModelType();

        AttributionResult result = new AttributionResult();
        result.setModelType(modelType.name().toLowerCase());

        List<TouchPointDTO> touchPoints = new ArrayList<>();
        Map<String, Double> channelWeights = new LinkedHashMap<>();

        if (channels == null || channels.isEmpty()) {
            result.setTouchPoints(touchPoints);
            result.setChannelWeights(channelWeights);
            return result;
        }

        switch (modelType) {
            case LINEAR:
                calculateLinear(channels, touchPoints, channelWeights);
                break;
            case POSITION:
                calculatePosition(channels, touchPoints, channelWeights, config.getWeightConfig());
                break;
            case LAST_CLICK:
            default:
                calculateLastClick(channels, touchPoints, channelWeights);
                break;
        }

        result.setTouchPoints(touchPoints);
        result.setChannelWeights(channelWeights);
        return result;
    }

    private void calculateLastClick(List<String> channels, List<TouchPointDTO> touchPoints, Map<String, Double> channelWeights) {
        String lastChannel = channels.get(channels.size() - 1);
        for (int i = 0; i < channels.size(); i++) {
            double weight = channels.get(i).equals(lastChannel) ? 1.0 : 0.0;
            touchPoints.add(new TouchPointDTO(channels.get(i), i + 1, weight));
        }
        channelWeights.put(lastChannel, 1.0);
        for (String ch : channels) {
            if (!ch.equals(lastChannel)) {
                channelWeights.putIfAbsent(ch, 0.0);
            }
        }
    }

    private void calculateLinear(List<String> channels, List<TouchPointDTO> touchPoints, Map<String, Double> channelWeights) {
        double weight = 1.0 / channels.size();
        for (int i = 0; i < channels.size(); i++) {
            touchPoints.add(new TouchPointDTO(channels.get(i), i + 1, weight));
            channelWeights.merge(channels.get(i), weight, Double::sum);
        }
    }

    private void calculatePosition(List<String> channels, List<TouchPointDTO> touchPoints, Map<String, Double> channelWeights, String weightConfig) {
        double firstWeight = 0.4;
        double lastWeight = 0.4;
        double middleWeight = 0.2;

        if (weightConfig != null && !weightConfig.isBlank()) {
            try {
                String[] parts = weightConfig.split(",");
                if (parts.length == 3) {
                    firstWeight = Double.parseDouble(parts[0].trim());
                    lastWeight = Double.parseDouble(parts[1].trim());
                    middleWeight = Double.parseDouble(parts[2].trim());
                }
            } catch (NumberFormatException ignored) {
            }
        }

        int n = channels.size();
        if (n == 1) {
            touchPoints.add(new TouchPointDTO(channels.get(0), 1, 1.0));
            channelWeights.put(channels.get(0), 1.0);
            return;
        }

        if (n == 2) {
            touchPoints.add(new TouchPointDTO(channels.get(0), 1, firstWeight));
            touchPoints.add(new TouchPointDTO(channels.get(1), 2, lastWeight));
            channelWeights.merge(channels.get(0), firstWeight, Double::sum);
            channelWeights.merge(channels.get(1), lastWeight, Double::sum);
            return;
        }

        double perMiddle = middleWeight / (n - 2);
        for (int i = 0; i < n; i++) {
            double w;
            if (i == 0) {
                w = firstWeight;
            } else if (i == n - 1) {
                w = lastWeight;
            } else {
                w = perMiddle;
            }
            touchPoints.add(new TouchPointDTO(channels.get(i), i + 1, w));
            channelWeights.merge(channels.get(i), w, Double::sum);
        }
    }

    private AttributionConfig resolveConfig(Long configId) {
        if (configId != null) {
            return attributionConfigRepository.findById(configId)
                    .orElseGet(this::createDefaultConfig);
        }
        return createDefaultConfig();
    }

    private AttributionConfig createDefaultConfig() {
        AttributionConfig config = new AttributionConfig();
        config.setName("default");
        config.setModelType(AttributionModelType.LAST_CLICK);
        return config;
    }

    public AttributionConfig createConfig(com.analytics.dto.AttributionConfigRequest request) {
        AttributionConfig config = new AttributionConfig();
        config.setName(request.getName());
        config.setModelType(AttributionModelType.valueOf(request.getModelType().toUpperCase()));
        config.setWeightConfig(request.getWeightConfig());
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return attributionConfigRepository.save(config);
    }

    public AttributionConfig updateConfig(Long id, com.analytics.dto.AttributionConfigRequest request) {
        AttributionConfig config = attributionConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribution config not found"));
        config.setName(request.getName());
        config.setModelType(AttributionModelType.valueOf(request.getModelType().toUpperCase()));
        config.setWeightConfig(request.getWeightConfig());
        config.setUpdatedAt(LocalDateTime.now());
        return attributionConfigRepository.save(config);
    }

    public List<AttributionConfig> getAllConfigs() {
        return attributionConfigRepository.findAll();
    }

    public Optional<AttributionConfig> getConfig(Long id) {
        return attributionConfigRepository.findById(id);
    }

    public void deleteConfig(Long id) {
        attributionConfigRepository.deleteById(id);
    }
}
