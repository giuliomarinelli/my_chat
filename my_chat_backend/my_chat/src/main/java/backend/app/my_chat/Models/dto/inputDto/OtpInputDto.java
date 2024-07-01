package backend.app.my_chat.Models.dto.inputDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpInputDto(
        @NotBlank(message = "OTP obbligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "OTP malformato. Un OTP è costituito da 6 cifre numeriche")
        String otp
) {}
