package app.utils;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class Validation {

    @SuppressWarnings("resource")
    private static final Validator VALIDATOR = buildDefaultValidatorFactory().getValidator();

    public static <T> T validate(T object, String message) {
        var constraintViolations = VALIDATOR.validate(object);
        if (!constraintViolations.isEmpty()) {
            if (message == null || message.isEmpty()) {
                throw new ValidationException(
                    message,
                    new ConstraintViolationException(constraintViolations)
                );
            } else {
                throw new ConstraintViolationException(constraintViolations);
            }
        }
        return object;
    }

    public static <T> T validate(T object) {
        return validate(object, null);
    }

}
