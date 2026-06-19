package com.analytics.service;

import com.analytics.dto.AttributionResult;
import com.analytics.entity.AttributionConfig;
import com.analytics.entity.AttributionModelType;
import com.analytics.exception.AttributionCalculationException;
import com.analytics.repository.AttributionConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributionServiceTest {

    @Mock
    private AttributionConfigRepository attributionConfigRepository;

    private AttributionService attributionService;

    @BeforeEach
    void setUp() {
        attributionService = new AttributionService(attributionConfigRepository);
    }

    @Test
    @DisplayName("线性归因模式下多触点权重之和必须等于1.0，浮点误差不超过0.001")
    void testLinearAttribution_WeightSumEqualsOne() {
        AttributionConfig config = new AttributionConfig();
        config.setId(1L);
        config.setName("linear-config");
        config.setModelType(AttributionModelType.LINEAR);
        when(attributionConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        List<String> channels5 = Arrays.asList("SEM", "社交", "邮件", "展示", "SEO");
        AttributionResult result5 = attributionService.calculate(channels5, 1L);
        double sum5 = result5.getTouchPoints().stream()
                .mapToDouble(tp -> tp.getWeight())
                .sum();
        assertTrue(Math.abs(sum5 - 1.0) < 0.001,
                "5个触点线性归因权重和应为1.0，实际: " + sum5);

        List<String> channels3 = Arrays.asList("A", "B", "C");
        AttributionResult result3 = attributionService.calculate(channels3, 1L);
        double sum3 = result3.getTouchPoints().stream()
                .mapToDouble(tp -> tp.getWeight())
                .sum();
        assertTrue(Math.abs(sum3 - 1.0) < 0.001,
                "3个触点线性归因权重和应为1.0，实际: " + sum3);

        List<String> channels1 = Collections.singletonList("唯一渠道");
        AttributionResult result1 = attributionService.calculate(channels1, 1L);
        double sum1 = result1.getTouchPoints().stream()
                .mapToDouble(tp -> tp.getWeight())
                .sum();
        assertTrue(Math.abs(sum1 - 1.0) < 0.001,
                "1个触点线性归因权重和应为1.0，实际: " + sum1);

        verify(attributionConfigRepository, times(3)).findById(1L);
    }

    @Test
    @DisplayName("touchpoints列表为空时应抛出AttributionCalculationException")
    void testEmptyTouchPoints_ThrowsAttributionCalculationException() {
        AttributionConfig config = new AttributionConfig();
        config.setId(1L);
        config.setName("linear-config");
        config.setModelType(AttributionModelType.LINEAR);
        when(attributionConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        assertThrows(AttributionCalculationException.class,
                () -> attributionService.calculate(Collections.emptyList(), 1L),
                "空列表应抛出AttributionCalculationException");

        assertThrows(AttributionCalculationException.class,
                () -> attributionService.calculate(null, 1L),
                "null列表应抛出AttributionCalculationException");
    }

    @Test
    @DisplayName("空列表异常应适用于所有归因模式，不仅限于线性")
    void testEmptyTouchPoints_AllModesThrowException() {
        AttributionConfig linearConfig = new AttributionConfig();
        linearConfig.setModelType(AttributionModelType.LINEAR);
        when(attributionConfigRepository.findById(1L)).thenReturn(Optional.of(linearConfig));
        assertThrows(AttributionCalculationException.class,
                () -> attributionService.calculate(Collections.emptyList(), 1L));

        AttributionConfig positionConfig = new AttributionConfig();
        positionConfig.setModelType(AttributionModelType.POSITION);
        when(attributionConfigRepository.findById(2L)).thenReturn(Optional.of(positionConfig));
        assertThrows(AttributionCalculationException.class,
                () -> attributionService.calculate(Collections.emptyList(), 2L));

        AttributionConfig lastClickConfig = new AttributionConfig();
        lastClickConfig.setModelType(AttributionModelType.LAST_CLICK);
        when(attributionConfigRepository.findById(3L)).thenReturn(Optional.of(lastClickConfig));
        assertThrows(AttributionCalculationException.class,
                () -> attributionService.calculate(Collections.emptyList(), 3L));
    }

    @Test
    @DisplayName("旧数据model_type为null时自动降级为last_click模式")
    void testModelTypeNull_FallsBackToLastClick() {
        AttributionConfig oldConfig = new AttributionConfig();
        oldConfig.setId(99L);
        oldConfig.setName("legacy-config");
        oldConfig.setModelType(null);
        when(attributionConfigRepository.findById(99L)).thenReturn(Optional.of(oldConfig));

        List<String> channels = Arrays.asList("百度", "谷歌", "必应");
        AttributionResult result = attributionService.calculate(channels, 99L);

        assertEquals("last_click", result.getModelType(),
                "modelType为null时结果modelType应为last_click");

        assertEquals(channels.size(), result.getTouchPoints().size());
        for (int i = 0; i < result.getTouchPoints().size() - 1; i++) {
            assertEquals(0.0, result.getTouchPoints().get(i).getWeight(), 0.001,
                    "非最后一个触点权重应为0");
        }
        assertEquals(1.0,
                result.getTouchPoints().get(result.getTouchPoints().size() - 1).getWeight(), 0.001,
                "最后一个触点权重应为1.0");

        assertEquals(1, result.getChannelWeights().entrySet().stream()
                .filter(e -> Math.abs(e.getValue() - 1.0) < 0.001).count());

        verify(attributionConfigRepository).findById(99L);
    }

    @Test
    @DisplayName("getEffectiveModelType直接验证：null -> LAST_CLICK")
    void testGetEffectiveModelType_NullFallsBack() {
        AttributionConfig nullModelConfig = new AttributionConfig();
        nullModelConfig.setModelType(null);
        AttributionModelType result = attributionService.getEffectiveModelType(nullModelConfig);
        assertEquals(AttributionModelType.LAST_CLICK, result);

        AttributionConfig linearConfig = new AttributionConfig();
        linearConfig.setModelType(AttributionModelType.LINEAR);
        assertEquals(AttributionModelType.LINEAR, attributionService.getEffectiveModelType(linearConfig));

        AttributionConfig positionConfig = new AttributionConfig();
        positionConfig.setModelType(AttributionModelType.POSITION);
        assertEquals(AttributionModelType.POSITION, attributionService.getEffectiveModelType(positionConfig));

        AttributionConfig lastClickConfig = new AttributionConfig();
        lastClickConfig.setModelType(AttributionModelType.LAST_CLICK);
        assertEquals(AttributionModelType.LAST_CLICK, attributionService.getEffectiveModelType(lastClickConfig));
    }

    @Test
    @DisplayName("线性归因：channelWeights聚合权重之和也必须等于1.0")
    void testLinearAttribution_ChannelWeightsSumEqualsOne() {
        AttributionConfig config = new AttributionConfig();
        config.setModelType(AttributionModelType.LINEAR);
        when(attributionConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        List<String> channels = Arrays.asList("SEM", "社交", "SEM", "邮件", "社交");
        AttributionResult result = attributionService.calculate(channels, 1L);

        double channelWeightSum = result.getChannelWeights().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        assertTrue(Math.abs(channelWeightSum - 1.0) < 0.001,
                "channelWeights聚合权重之和应为1.0，实际: " + channelWeightSum);

        double touchPointWeightSum = result.getTouchPoints().stream()
                .mapToDouble(tp -> tp.getWeight())
                .sum();
        assertTrue(Math.abs(touchPointWeightSum - 1.0) < 0.001,
                "touchPoints权重之和应为1.0，实际: " + touchPointWeightSum);
    }
}
