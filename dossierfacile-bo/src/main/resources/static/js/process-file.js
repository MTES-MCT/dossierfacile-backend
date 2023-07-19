$(document).keydown(function (e) {
    //ctrl+enter
    var flag = true;
    if (e.ctrlKey && e.shiftKey && e.keyCode === 13) {
        e.preventDefault();
        flag = false;
        $('#finishProcessButton').click();
    }
    if (e.ctrlKey && e.keyCode === 13 && flag) {
        e.preventDefault();
        $("#validDecline").click();
    }
    //ctrl+3
    if (e.ctrlKey && e.keyCode === 51) {
        e.preventDefault();
        $('#checkGlobal1').click()
    }
    //ctrl+4
    if (e.ctrlKey && e.keyCode === 52) {
        e.preventDefault();
        $('#checkGlobal2').click()
    }
    //ctrl+5
    if (e.ctrlKey && e.keyCode === 53) {
        e.preventDefault();
        $('#checkGlobal3').click()
    }
    if (e.ctrlKey && e.keyCode === 54) {
        e.preventDefault();
        $('#checkRPGD').click()
    }
});

var textArea = true;
$(document).on("keydown", function (event) {
    //ctrl+1
    if ((event.which == 49 && event.ctrlKey)) {
        event.preventDefault();
        $('#checkbox1').click();
        //ctrl+2
    } else if ((event.which == 50 && event.ctrlKey)) {
        event.preventDefault();
        $('#check11').click();
        //ctrl+u
    } else if ((event.which == 85 && event.ctrlKey)) {
        event.preventDefault();
        if (textArea) {
            $('#text').focus();
        } else {
            $('#text1').focus();
        }
        textArea = !textArea;
        //ctrl+ú special character in keyboard french
    } else if ((event.which == 151 && event.ctrlKey)) {
        event.preventDefault();
        if (textArea) {
            $('#text').focus();
        } else {
            $('#text1').focus();
        }
        textArea = !textArea;
        //tab
    } else if (event.which == 9) {
        event.preventDefault();
        $('.document-embed').focus();
    }
});
$(document).ready(function () {
    $('.validDeclineLink').click(function (e) {
        e.preventDefault();
        $('#validDecline').click();
    });

   function updateStatus() {
       var list=[];
       var checkboxChecked = $('[type="checkbox"]').filter((k,v) => v.checked === true);

       checkboxChecked.each((k,v) => {
           var content=v.nextElementSibling.nextElementSibling.value;
           list.push(content);
       })

       var textareaList = $('textarea').filter((k, v) => v.value.length > 0);

       if (checkboxChecked.length > 0 || textareaList.length > 0) {
         $('#validation-status').css('background-color', 'red');
       } else {
         $('#validation-status').css('background-color', 'green');
       }
       $('#validation-status')[0].title = list.join('\n');
   }
   $('[type="checkbox"]').change(function() {
     updateStatus();
   });
   $('textarea').on('change keyup paste', function() {
     updateStatus();
   });

});
