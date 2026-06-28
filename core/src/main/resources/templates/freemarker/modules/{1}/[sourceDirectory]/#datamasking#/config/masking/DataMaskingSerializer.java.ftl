package ${packagePath};

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class DataMaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private MaskType maskType;

    public DataMaskingSerializer() {}

    public DataMaskingSerializer(MaskType maskType) {
        this.maskType = maskType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            gen.writeNull();
            return;
        }
        String maskedValue = value;
        switch (maskType) {
            case MOBILE:
                if (value.length() == 11) {
                    maskedValue = value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
                }
                break;
            case ID_CARD:
                if (value.length() == 15 || value.length() == 18) {
                    maskedValue = value.replaceAll("(?<=\\w{3})\\w(?=\\w{4})", "*");
                }
                break;
            case EMAIL:
                int index = value.indexOf("@");
                if (index > 1) {
                    maskedValue = value.substring(0, 1) + "***" + value.substring(index - 1);
                }
                break;
            case PASSWORD:
                maskedValue = "******";
                break;
            default:
                break;
        }
        gen.writeString(maskedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            DataMasking annotation = property.getAnnotation(DataMasking.class);
            if (annotation == null) {
                annotation = property.getContextAnnotation(DataMasking.class);
            }
            if (annotation != null) {
                return new DataMaskingSerializer(annotation.value());
            }
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
