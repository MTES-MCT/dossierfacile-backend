$(document).keydown(function (event) {
    if (event.ctrlKey && event.keyCode === 13) {
        $('#customMessageButton').click();
        event.preventDefault();
    }
    if (event.key === 'Escape') {
        window.location.replace("/bo");
    }
});

$(document).ready(function () {
    $('#customMessageButtonMobile').click(function (e) {
        e.preventDefault();
        $('#customMessageButton').click();
    });
    $("#customMessageButtonMobileDecline").click(function (e) {
        e.preventDefault();
        $('#form-decline').submit();
    })
});
