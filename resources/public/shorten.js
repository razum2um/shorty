$(document).ready(function() {
  var $form = $('#shorten');
  var $result = $('#result');
  var $shortened = $('#shortened');
  $form.submit(function(e) {
    e.preventDefault();
    $.post($form.attr('action'), $form.serialize(), function(data) {
      $shortened.html('<a target="_blank" href="' + data + '">' + data + '</a>');
    }).fail(function(xhr) {
      $shortened.html(xhr.responseText);
    }).always(function () {
      $result.show();
    });
    return false;
  });
});
