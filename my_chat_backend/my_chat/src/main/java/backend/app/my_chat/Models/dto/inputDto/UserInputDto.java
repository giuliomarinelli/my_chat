package backend.app.my_chat.Models.dto.inputDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserInputDto(

        @NotBlank(message = "Username obbligatorio")
        @Pattern(
                regexp = "^[\\w.\\-_]{3,}$", message = "Formato username non valido. " +
                "Sono ammessi solo caratteri alfanumerici, ., -, _ e la lunghezza minima è di 3 caratteri"
        )
        String username,

        @NotBlank(message = "Password obbligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!^.\\-_]).{12,}$",
                message = "Formato password non valido. La password deve contenere almeno una lettera minuscola, " +
                        "almeno una lettera maiuscola, almeno un numero, almeno un carattere tra !, ^, ., - e _ " +
                        "e deve essere lunga almeno 12 caratteri"
        )
        String password,

        @NotBlank(message = "Numero di telefono obbligatorio")
        @Pattern(
                regexp = "^\\d{9,10}$",
                message = "Numero di telefono non valido. Sono ammessi solo da 9 a 10 caratteri numerici"
        )
        String phoneNumber


) {
}