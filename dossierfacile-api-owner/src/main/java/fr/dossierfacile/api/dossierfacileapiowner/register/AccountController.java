package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.log.LogService;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerModel;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/register")
public class AccountController {

    private final RegisterService registerService;
    private final LogService logService;

    @PostMapping("/account")
    public ResponseEntity<OwnerModel> account(@RequestBody AccountForm accountForm) {
        OwnerModel ownerModel = registerService.register(accountForm);
//        logService.saveLog(LogType.ACCOUNT_CREATED, ownerModel.getId());
        return ok(ownerModel);
    }

    @GetMapping("/confirmAccount/{token}")
    public ResponseEntity<Void> confirmAccount(@PathVariable String token) {
        long ownerId = registerService.confirmAccount(token);
        logService.saveLog(LogType.EMAIL_ACCOUNT_VALIDATED, ownerId);
        return ok().build();
    }

    @PostMapping(value = "/forgotPassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPassword(@Validated @RequestBody EmailResetForm email) {
        registerService.forgotPassword(email.getEmail());
        return ok().build();
    }

    @PostMapping(value = "/createPassword/{token}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OwnerModel> createPassword(@PathVariable String token, @Validated @RequestBody PasswordForm password) {
        OwnerModel ownerModel = registerService.createPassword(token, password.getPassword());
        return ok(ownerModel);
    }


}
