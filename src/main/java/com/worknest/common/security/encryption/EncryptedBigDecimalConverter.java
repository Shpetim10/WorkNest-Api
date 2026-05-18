package com.worknest.common.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Converter
@RequiredArgsConstructor
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private final EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : encryptionService.encrypt(attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        String decrypted = encryptionService.decrypt(dbData);
        return decrypted == null ? null : new BigDecimal(decrypted);
    }
}
