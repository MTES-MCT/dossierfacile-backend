$(document).ready(function () {
    if ($('input[name=guarantorType]:checked').val() != 'NONE') {
        $("#guarantorFormEdit").show();
    }

    var defaultValueRadio = $('input[name=guarantorType]:checked').val();
    $('input[type=radio][name=guarantorType]').change(function () {
        if (this.value != defaultValueRadio && this.value != 'NONE') {
            $("#guarantorFormEdit").hide();
            if (this.value == 'LOCATIO') {
                $("#guarantorFormNew2").hide();
                $("#guarantorFormNew").show();
                $("#lastNameGuarantorNew").attr('name', 'guarantor.lastName');
                $("#firstNameGuarantorNew").attr('name', 'guarantor.firstName')
            } else {
                $("#guarantorFormNew").hide();
                $("#guarantorFormNew2").show();
            }
        }
        if (this.value == defaultValueRadio && this.value != 'NONE') {
            $("#guarantorFormNew").hide();
            $("#guarantorFormNew2").hide();
            $("#guarantorFormEdit").show();
        }
        if (this.value == 'NONE') {
            $("#guarantorFormNew").hide();
            $("#guarantorFormNew2").hide();
            $("#guarantorFormEdit").hide();
        }
    });
});
