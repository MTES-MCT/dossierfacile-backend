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
    document.getElementById('validDecline').addEventListener('click', function () {
      var timeSpent = (new Date() - startTime) + Number(document.getElementById('timeSpent').value);
      document.getElementById('timeSpent').value = timeSpent;
      console.log('time spent in ms = ' + timeSpent);
      document.getElementById('processFileForm').submit();
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