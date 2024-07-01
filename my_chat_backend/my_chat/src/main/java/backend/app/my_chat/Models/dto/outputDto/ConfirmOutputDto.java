package backend.app.my_chat.Models.dto.outputDto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class ConfirmOutputDto {

    private int statusCode;
    private Timestamp timestamp;
    private String message;

    public ConfirmOutputDto(HttpStatus statusCode, String message) {
        this.statusCode = statusCode.value();
        timestamp = Timestamp.valueOf(LocalDateTime.now());
        this.message = message;
    }
}
