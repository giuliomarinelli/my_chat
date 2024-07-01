package backend.app.my_chat.exception_handling;

import backend.app.my_chat.Models.dto.outputDto.HttpErrorOutputDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionFilter extends ResponseEntityExceptionHandler {

    private String getPath() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest req;
        if (requestAttributes instanceof ServletRequestAttributes) {
            req = ((ServletRequestAttributes) requestAttributes).getRequest();
        } else return "unkwnown";
        return req.getRequestURI();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<HttpErrorOutputDto> badRequestExceptionHandler(BadRequestException e) {
        return new ResponseEntity<>(new HttpErrorOutputDto(
                HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage(), getPath()
        ), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<HttpErrorOutputDto> unauthorizedExceptionHandler(UnauthorizedException e) {
        return new ResponseEntity<>(new HttpErrorOutputDto(
                HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage(), getPath()
        ), HttpStatus.UNAUTHORIZED);
    }

}
