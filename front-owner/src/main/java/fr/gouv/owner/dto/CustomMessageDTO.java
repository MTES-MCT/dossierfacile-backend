package fr.gouv.owner.dto;

import fr.dossierfacile.common.entity.Tenant;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Getter
@Setter
@Slf4j
public class CustomMessageDTO implements Serializable {

    private static final String GLOBAL1 = "bo.tenant.custom.email.global1";
    private static final String GLOBAL2 = "bo.tenant.custom.email.global2";
    private static final String GLOBAL2GUARANTOR = "bo.tenant.custom.email.global2.guarantor";
    private static final String GLOBAL3 = "bo.tenant.custom.email.global3";
    private static final String GLOBAL4 = "bo.tenant.custom.email.global4";
    private static final String GLOBAL4GUARANTOR = "bo.tenant.custom.email.global4.guarantor";
    private static final String RPGD = "bo.tenant.custom.email.RPGD";
    private static final String BLUR = "bo.tenant.custom.email.blur";
    private static final String IS_CHECK = "isCheck";
    private static final String GET_CHECK = "getCheck";
    private final transient int[] cantExtraCheck = new int[]{0, 1, 0, 0, 1, 0, 1, 0, 0, 0};
    private final transient int[] cantExtraText = new int[]{0, 0, 0, 0, 1, 0, 0, 0, 0, 0};
    private boolean check1 = false;
    private transient String check1Text = "bo.tenant.custom.email.option1";
    private boolean check1Global1 = false;
    private transient String check1Global1Text = GLOBAL1;
    private boolean check1Global2 = false;
    private transient String check1Global2Text = GLOBAL2;
    private boolean check1Global3 = false;
    private transient String check1Global3Text = GLOBAL3;
    private boolean check1Global4 = false;
    private transient String check1Global4Text = GLOBAL4;
    private boolean check1RPGD = false;
    private transient String check1RPGDText = RPGD;
    private boolean check1Blur = false;
    private transient String check1BlurText = BLUR;
    private boolean check2 = false;
    private transient String check2Text = "bo.tenant.custom.email.option2";
    private boolean check21 = false;
    private transient String check21Text = "bo.tenant.custom.email.option21";
    private boolean check2Global1 = false;
    private transient String check2Global1Text = GLOBAL1;
    private boolean check2Global2 = false;
    private transient String check2Global2Text = GLOBAL2;
    private transient String check2Global2TextGuarantor = GLOBAL2;
    private boolean check2Global3 = false;
    private transient String check2Global3Text = GLOBAL3;
    private boolean check2Global4 = false;
    private transient String check2Global4Text = GLOBAL4;
    private boolean check2RPGD = false;
    private transient String check2RPGDText = RPGD;
    private boolean check2Blur = false;
    private transient String check2BlurText = BLUR;
    private boolean check3 = false;
    private transient String check3Text = "bo.tenant.custom.email.option3";
    private boolean check3Global1 = false;
    private transient String check3Global1Text = GLOBAL1;
    private boolean check3Global2 = false;
    private transient String check3Global2Text = GLOBAL2;
    private boolean check3Global3 = false;
    private transient String check3Global3Text = GLOBAL3;
    private boolean check3Global4 = false;
    private transient String check3Global4Text = GLOBAL4;
    private boolean check3RPGD = false;
    private transient String check3RPGDText = RPGD;
    private boolean check3Blur = false;
    private transient String check3BlurText = BLUR;
    private boolean check4 = false;
    private transient String check4Text = "bo.tenant.custom.email.option4";
    private boolean check4Global1 = false;
    private transient String check4Global1Text = GLOBAL1;
    private boolean check4Global2 = false;
    private transient String check4Global2Text = GLOBAL2;
    private boolean check4Global3 = false;
    private transient String check4Global3Text = GLOBAL3;
    private boolean check4Global4 = false;
    private transient String check4Global4Text = GLOBAL4;
    private boolean check4RPGD = false;
    private transient String check4RPGDText = RPGD;
    private boolean check4Blur = false;
    private transient String check4BlurText = BLUR;
    private boolean check5 = false;
    private transient String check5Text = "bo.tenant.custom.email.option5";
    private boolean check5Global1 = false;
    private transient String check5Global1Text = GLOBAL1;
    private boolean check5Global2 = false;
    private transient String check5Global2Text = GLOBAL2;
    private boolean check5Global3 = false;
    private transient String check5Global3Text = GLOBAL3;
    private boolean check5Global4 = false;
    private transient String check5Global4Text = GLOBAL4;
    private boolean check5RPGD = false;
    private transient String check5RPGDText = RPGD;
    private boolean check5Blur = false;
    private transient String check5BlurText = BLUR;
    private boolean check51 = false;
    private transient String check51Text = "bo.tenant.custom.email.option51";
    private boolean check6 = false;
    private transient String check6Text = "bo.tenant.custom.email.option6";
    private transient String check6Text1 = "bo.tenant.custom.email.option6text1";
    private boolean check6Global1 = false;
    private transient String check6Global1Text = GLOBAL1;
    private boolean check6Global2 = false;
    private transient String check6Global2Text = GLOBAL2GUARANTOR;
    private boolean check6Global3 = false;
    private transient String check6Global3Text = GLOBAL3;
    private boolean check6Global4 = false;
    private transient String check6Global4Text = GLOBAL4GUARANTOR;
    private boolean check6RPGD = false;
    private transient String check6RPGDText = RPGD;
    private boolean check6Blur = false;
    private transient String check6BlurText = BLUR;
    private boolean check7 = false;
    private transient String check7Text = "bo.tenant.custom.email.option7";
    private boolean check71 = false;
    private transient String check71Text = "bo.tenant.custom.email.option71";
    private boolean check7Global1 = false;
    private transient String check7Global1Text = GLOBAL1;
    private boolean check7Global2 = false;
    private transient String check7Global2Text = GLOBAL2GUARANTOR;
    private boolean check7Global3 = false;
    private transient String check7Global3Text = GLOBAL3;
    private boolean check7Global4 = false;
    private transient String check7Global4Text = GLOBAL4GUARANTOR;
    private boolean check7RPGD = false;
    private transient String check7RPGDText = RPGD;
    private boolean check7Blur = false;
    private transient String check7BlurText = BLUR;
    private boolean check8 = false;
    private transient String check8Text = "bo.tenant.custom.email.option8";
    private boolean check8Global1 = false;
    private transient String check8Global1Text = GLOBAL1;
    private boolean check8Global2 = false;
    private transient String check8Global2Text = GLOBAL2GUARANTOR;
    private boolean check8Global3 = false;
    private transient String check8Global3Text = GLOBAL3;
    private boolean check8Global4 = false;
    private transient String check8Global4Text = GLOBAL4GUARANTOR;
    private boolean check8RPGD = false;
    private transient String check8RPGDText = RPGD;
    private boolean check8Blur = false;
    private transient String check8BlurText = BLUR;
    private boolean check9 = false;
    private transient String check9Text = "bo.tenant.custom.email.option9";
    private boolean check9Global1 = false;
    private transient String check9Global1Text = GLOBAL1;
    private boolean check9Global2 = false;
    private transient String check9Global2Text = GLOBAL2GUARANTOR;
    private boolean check9Global3 = false;
    private transient String check9Global3Text = GLOBAL3;
    private boolean check9Global4 = false;
    private transient String check9Global4Text = GLOBAL4GUARANTOR;
    private boolean check9RPGD = false;
    private transient String check9RPGDText = RPGD;
    private boolean check9Blur = false;
    private transient String check9BlurText = BLUR;
    private boolean check10 = false;
    private transient String check10Text = "bo.tenant.custom.email.option10";
    private boolean check10Global1 = false;
    private transient String check10Global1Text = GLOBAL1;
    private boolean check10Global2 = false;
    private transient String check10Global2Text = GLOBAL2GUARANTOR;
    private boolean check10Global3 = false;
    private transient String check10Global3Text = GLOBAL3;
    private boolean check10Global4 = false;
    private transient String check10Global4Text = GLOBAL4GUARANTOR;
    private boolean check10RPGD = false;
    private transient String check10RPGDText = RPGD;
    private boolean check10Blur = false;
    private transient String check10BlurText = BLUR;
    private String text1 = "";
    private String text2 = "";
    private String text3 = "";
    private String text4 = "";
    private String text5 = "";
    private String text51 = "";
    private String text6 = "";
    private String text7 = "";
    private String text8 = "";
    private String text9 = "";
    private String text10 = "";
    private LocalDateTime localDateTime = LocalDateTime.now();

    public CustomMessageDTO() {
    }

    public CustomMessageDTO(CustomMessageDTO last) {
        this.check1 = last.check1;
        this.check1Global1 = last.check1Global1;
        this.check1Global2 = last.check1Global2;
        this.check1Global3 = last.check1Global3;
        this.check1Global4 = last.check1Global4;
        this.check1RPGD = last.check1RPGD;
        this.check1Blur = last.check1Blur;

        this.check2 = last.check2;
        this.check21 = last.check21;
        this.check2Global1 = last.check2Global1;
        this.check2Global2 = last.check2Global2;
        this.check2Global3 = last.check2Global3;
        this.check2Global4 = last.check2Global4;
        this.check2RPGD = last.check2RPGD;
        this.check2Blur = last.check2Blur;

        this.check3 = last.check3;
        this.check3Global1 = last.check3Global1;
        this.check3Global2 = last.check3Global2;
        this.check3Global3 = last.check3Global3;
        this.check3Global4 = last.check3Global4;
        this.check3RPGD = last.check3RPGD;
        this.check3Blur = last.check3Blur;

        this.check4 = last.check4;
        this.check4Global1 = last.check4Global1;
        this.check4Global2 = last.check4Global2;
        this.check4Global3 = last.check4Global3;
        this.check4Global4 = last.check4Global4;
        this.check4RPGD = last.check4RPGD;
        this.check4Blur = last.check4Blur;

        this.check5 = last.check5;
        this.check5Global1 = last.check5Global1;
        this.check5Global2 = last.check5Global2;
        this.check5Global3 = last.check5Global3;
        this.check5Global4 = last.check5Global4;
        this.check5RPGD = last.check5RPGD;
        this.check5Blur = last.check5Blur;
        this.check51 = last.check51;

        this.check6 = last.check6;
        this.check6Global1 = last.check6Global1;
        this.check6Global2 = last.check6Global2;
        this.check6Global3 = last.check6Global3;
        this.check6Global4 = last.check6Global4;
        this.check6RPGD = last.check6RPGD;
        this.check6Blur = last.check6Blur;

        this.check7 = last.check7;
        this.check71 = last.check71;
        this.check7Global1 = last.check7Global1;
        this.check7Global2 = last.check7Global2;
        this.check7Global3 = last.check7Global3;
        this.check7Global4 = last.check7Global4;
        this.check7RPGD = last.check7RPGD;
        this.check7Blur = last.check7Blur;

        this.check8 = last.check8;
        this.check8Global1 = last.check8Global1;
        this.check8Global2 = last.check8Global2;
        this.check8Global3 = last.check8Global3;
        this.check8Global4 = last.check8Global4;
        this.check8RPGD = last.check8RPGD;
        this.check8Blur = last.check8Blur;

        this.check9 = last.check9;
        this.check9Global1 = last.check9Global1;
        this.check9Global2 = last.check9Global2;
        this.check9Global3 = last.check9Global3;
        this.check9Global4 = last.check9Global4;
        this.check9RPGD = last.check9RPGD;
        this.check9Blur = last.check9Blur;

        this.check10 = last.check10;
        this.check10Global1 = last.check10Global1;
        this.check10Global2 = last.check10Global2;
        this.check10Global3 = last.check10Global3;
        this.check10Global4 = last.check10Global4;
        this.check10RPGD = last.check10RPGD;
        this.check10Blur = last.check10Blur;

        this.text1 = last.text1;
        this.text2 = last.text2;
        this.text3 = last.text3;
        this.text4 = last.text4;
        this.text5 = last.text5;
        this.text51 = last.text51;
        this.text6 = last.text6;
        this.text7 = last.text7;
        this.text8 = last.text8;
        this.text9 = last.text9;
        this.text10 = last.text10;
    }

    public boolean isGuarantor() {
        return check6 || check7 || check71 || check8 || check9 || check10 || !text6.equals("") || !text7.equals("")
                || !text8.equals("") || !text9.equals("") || !text10.equals("")
                || check6Global1 || check6Global2 || check6Global3 || check6Global4
                || check7Global1 || check7Global2 || check7Global3 || check7Global4
                || check8Global1 || check8Global2 || check8Global3 || check8Global4
                || check9Global1 || check9Global2 || check9Global3 || check9Global4
                || check10Global1 || check10Global2 || check10Global3 || check10Global4
                || check6RPGD || check7RPGD || check8RPGD || check9RPGD || check10RPGD
                || check6Blur || check7Blur || check8Blur || check9Blur || check10Blur;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public boolean isCheck(int number) {
        try {
            Method is = CustomMessageDTO.class.getDeclaredMethod(IS_CHECK + number);
            return (boolean) is.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public boolean isCheckGlobal(int checkNumber, int globalNumber) {
        try {
            Method is = CustomMessageDTO.class.getDeclaredMethod(IS_CHECK + checkNumber + "Global" + globalNumber);
            return (boolean) is.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public boolean isCheckRPGD(int number) {
        try {
            Method is = CustomMessageDTO.class.getDeclaredMethod(IS_CHECK + number + "RPGD");
            return (boolean) is.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public boolean isCheckBlur(int number) {
        try {
            Method is = CustomMessageDTO.class.getDeclaredMethod(IS_CHECK + number + "Blur");
            return (boolean) is.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public String getCheckText(int number, Tenant tenant) {
        if (number == 3) {
            return tenant.getTenantSituation().getMessageFile3();
        }
        if (number == 5) {
            return tenant.getTenantSituation().getMessageFile5();
        }
        try {
            Method get = CustomMessageDTO.class.getDeclaredMethod(GET_CHECK + number + "Text");
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    public String getCheckGlobalText(int checkNumber, int globalNumber) {
        try {
            Method get = CustomMessageDTO.class.getDeclaredMethod(GET_CHECK + checkNumber + "Global" + globalNumber + "Text");
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    public String getCheckRPGDText(int checkNumber) {
        try {
            Method get = CustomMessageDTO.class.getDeclaredMethod(GET_CHECK + checkNumber + "RPGDText");
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    public String getCheckBlurText(int checkNumber) {
        try {
            Method get = CustomMessageDTO.class.getDeclaredMethod(GET_CHECK + checkNumber + "BlurText");
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    public String getText(int number) {
        try {
            Method get = CustomMessageDTO.class.getDeclaredMethod("getText" + number);
            return (String) get.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    public boolean isFileWithMessage(int number) {
        boolean result = isCheck(number)
                || isCheckGlobal(number, 1)
                || isCheckGlobal(number, 2)
                || isCheckGlobal(number, 3)
                || isCheckGlobal(number, 4)
                || isCheckRPGD(number)
                || isCheckBlur(number)
                || !getText(number).equals("");

        int cantCheck = cantExtraCheck(number);
        int cantText = cantExtraText(number);
        for (int j = 1; j <= cantCheck; j++) {
            result = result || isCheck(number * 10 + j);
        }
        for (int j = 1; j <= cantText; j++) {
            result = result || !getText(number * 10 + j).isEmpty();
        }
        return result;
    }

    public int cantExtraCheck(int checkNumber) {
        return cantExtraCheck[checkNumber - 1];
    }

    public int cantExtraText(int checkNumber) {
        return cantExtraText[checkNumber - 1];
    }

}
