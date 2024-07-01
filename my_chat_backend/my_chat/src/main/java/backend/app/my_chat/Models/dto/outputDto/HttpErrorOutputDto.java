package backend.app.my_chat.Models.dto.outputDto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class HttpErrorOutputDto {
    private Timestamp timestamp;
    private int statusCode;
    private String error;
    private String message;
    private String path;

    public HttpErrorOutputDto(HttpStatus httpStatus, String error, String message, String path) {
        timestamp = Timestamp.valueOf(LocalDateTime.now());
        this.statusCode = httpStatus.value();
        this.error = error;
        this.message = message;
        this.path = path;
    }
}