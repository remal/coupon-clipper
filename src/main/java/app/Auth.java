package app;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@SuperBuilder
public class Auth {

    @NotEmpty
    String login;

    @NotEmpty
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    String password;

}
