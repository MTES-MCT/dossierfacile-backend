$(document).keydown(function (event) {
    if (event.ctrlKey && event.keyCode === 13) {
        document.location = $('#nextApplication').attr("href");
        event.preventDefault();
    }
});
