package fr.gouv.bo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProcessFileDTO {

    private boolean check = false;
    private boolean checkGlobal1 = false;
    private boolean checkGlobal2 = false;
    private boolean checkGlobal3 = false;
    private boolean checkGlobal4 = false;
    private boolean checkRPGD = false;
    private boolean checkBlur = false;

    private String text;
    private int fileNumber;

    private boolean check1 = false;
    private String text1;

    private LocalDateTime initDate = LocalDateTime.now();

    public ProcessFileDTO(int fileNumber) {
        this.fileNumber = fileNumber;
    }

    public boolean isValid() {
        return !check && !check1 && !checkGlobal1 && !checkGlobal2 && !checkGlobal3 && !checkGlobal4 && !checkRPGD && !checkBlur && (text == null || text.equals("")) && (text1 == null || text1.equals(""));
    }

    public boolean idDeniedWithCheckbox() {
        return check || check1 || checkGlobal1 || checkGlobal2 || checkGlobal3 || checkGlobal4 || checkRPGD || checkBlur;
    }

    public boolean idDeniedWithText() {
        return (text != null && !text.equals("")) || (text1 != null && !text1.equals(""));
    }

    public void setCheck(int numberCheck, boolean value) {
        try {
            Method method = ProcessFileDTO.class.getDeclaredMethod("setCheck" + numberCheck, Boolean.class);
            method.invoke(this, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
        }
    }

    public void setText(int numberCheck, String value) {
        try {
            Method method = ProcessFileDTO.class.getDeclaredMethod("setText" + numberCheck, String.class);
            method.invoke(this, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
        }
    }

    public boolean isCheck(int checkNumber) {
        try {
            Method is = ProcessFileDTO.class.getDeclaredMethod("isCheck" + checkNumber);
            return (boolean) is.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public String getText(int number) {
        try {
            Method get = ProcessFileDTO.class.getDeclaredMethod("getText" + number);
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }
}
