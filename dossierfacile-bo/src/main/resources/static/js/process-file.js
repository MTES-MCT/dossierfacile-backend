function showAmountForm($parent, editing) {
  $parent.find(".input-amount, .cancel-update-amount, .validate-amount").toggle(editing);
  $parent.find(".change-amount").toggle(!editing);
}

function clearAmountError($parent) {
  $parent.find(".amount-error").prop("hidden", true);
  $parent.find(".input-amount").removeAttr("aria-invalid");
}

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
    let processFileForm = document.getElementById('processFileForm');
    let originalAction = processFileForm.action;

    document.getElementById('validDecline').addEventListener('click', function (event) {
      event.preventDefault();
      if (!validateMonthlySums()) return;
      const timeSpent = (Date.now() - startTime) + Number(document.getElementById('timeSpent').value);
      document.getElementById('timeSpent').value = timeSpent;
      console.log('time spent in ms = ' + timeSpent);
      // Restore original action for "Envoyer" button
      processFileForm.action = originalAction;
      processFileForm.submit();
    });

    document.getElementById('validDeclineAndClose').addEventListener('click', function () {
      if (!validateMonthlySums()) return;
      const timeSpent = (Date.now() - startTime) + Number(document.getElementById('timeSpent').value);
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

   function isAmountValid($parent) {
     var input = $parent.find(".input-amount")[0];
     if (input.checkValidity() && /^\d+$/.test(input.value)) {
       clearAmountError($parent);
       return true;
     }
     showAmountForm($parent, true);
     $parent.find(".amount-error").prop("hidden", false);
     input.setAttribute("aria-invalid", "true");
     input.focus();
     return false;
   }

   function validateMonthlySums() {
     for (var amount of processFileForm.querySelectorAll(".monthly-amount")) {
       var prefix = amount.dataset.fieldPrefix;
       var input = amount.querySelector(".input-amount");
       var previousAmount = processFileForm.elements.namedItem(prefix + ".monthlySum");
       var comment = processFileForm.elements.namedItem(prefix + ".commentDoc");
       var denied = (comment?.value.length > 0) ||
         Array.from(processFileForm.querySelectorAll('input[type="checkbox"]')).some(function (reason) {
           return reason.name.startsWith(prefix + ".itemDetailList[") &&
             reason.name.endsWith(".check") && reason.checked;
         });
       if (previousAmount?.value === "" && input.value === "" && !input.validity.badInput && denied) {
         clearAmountError($(amount));
         continue;
       }
       if (!isAmountValid($(amount))) return false;
     }
     return true;
   }

   $(".change-amount").on("click", function () {
     var $parent = $(this).parent();
     showAmountForm($parent, true);
     $parent.find(".input-amount").trigger("focus");
   });

   function validateAmount($parent) {
     if (!isAmountValid($parent)) return;
     var $input = $parent.find(".input-amount");
     var $textAmount = $parent.find(".text-amount");
     $textAmount.text("Montant mensuel : " + $input.val());
     $input.attr("value", $input.val());
     $parent.find(".change-amount").text("Modifier le montant");
     showAmountForm($parent, false);
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
     $textAmount.text("Montant mensuel : " + ($input.val() || "non renseigné"));
     clearAmountError($parent);
     showAmountForm($parent, false);
   });

});
