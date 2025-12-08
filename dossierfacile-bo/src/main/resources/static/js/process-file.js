$(document).keydown(function (e) {
    //ctrl+enter
    if (e.ctrlKey && e.keyCode === 13) {
        e.preventDefault();
        $("#validDecline").click();
    }
});

var textArea = true;
var startTime = null;
$(document).ready(function () {

    startTime = new Date();
    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState === 'visible') {
        startTime = new Date() ;
      } else {
        if (startTime) {
          var timeSpent = (new Date() - startTime) + Number(document.getElementById('timeSpent').value);
          document.getElementById('timeSpent').value = timeSpent;
        }
      }
    });
    var processFileForm = document.getElementById('processFileForm');
    var originalAction = processFileForm.action;

    document.getElementById('validDecline').addEventListener('click', function () {
      var timeSpent = (new Date() - startTime) + Number(document.getElementById('timeSpent').value);
      document.getElementById('timeSpent').value = timeSpent;
      console.log('time spent in ms = ' + timeSpent);
      // Restore original action for "Envoyer" button
      processFileForm.action = originalAction;
      processFileForm.submit();
    });

    document.getElementById('validDeclineAndClose').addEventListener('click', function () {
      var timeSpent = (new Date() - startTime) + Number(document.getElementById('timeSpent').value);
      document.getElementById('timeSpent').value = timeSpent;
      console.log('time spent in ms = ' + timeSpent);
      // Add returnToHome parameter to action to processFile endpoint
      processFileForm.action = processFileForm.action + '?returnToHome=true';
      processFileForm.submit();
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

   function toggleForm($parent) {
     var $input = $parent.find(".input-amount");
     var $cancelButton = $parent.find(".cancel-update-amount");
     var $validateButton = $parent.find(".validate-amount");
     var $changeButton = $parent.find(".change-amount");

     $input.toggle();
     $cancelButton.toggle();
     $validateButton.toggle();
     $changeButton.toggle();
   }

   $(".change-amount").on("click", function () {
     var $parent = $(this).parent();
     var $textAmount = $parent.find(".text-amount");
     $textAmount.text($textAmount.text().replace(/\d+$/, ""));
     toggleForm($parent);
   });

   function validateAmount($parent) {
     var $input = $parent.find(".input-amount");
     var $textAmount = $parent.find(".text-amount");
     $textAmount.text($textAmount.text() + $input.val());
     $input.attr("value", $input.val());
     toggleForm($parent);
   }

   $(".validate-amount").on("click", function () {
     validateAmount($(this).parent());
   });

   $(".input-amount").on("keypress", function (event) {
     if (event.key === "Enter") {
       event.preventDefault();
       validateAmount($(this).parent());
     }
   });

   $(".cancel-update-amount").on("click", function () {
     var $parent = $(this).parent();
     var $input = $parent.find(".input-amount");
     var $textAmount = $parent.find(".text-amount");
     $input.val($input.attr("value"));
     $textAmount.text($textAmount.text() + $input.val());
     toggleForm($parent);
   });

});