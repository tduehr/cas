package org.apereo.cas.util.serialization;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.*;

@Converter(autoApply = true)
public class ZonedDateTimeConvertor implements AttributeConverter<ZonedDateTime, String> {

    @Override
    public String convertToDatabaseColumn(ZonedDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.format(ISO_ZONED_DATE_TIME);
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ZonedDateTime.parse(dbData, ISO_ZONED_DATE_TIME);
    }
}
