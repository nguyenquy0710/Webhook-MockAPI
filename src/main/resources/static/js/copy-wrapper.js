$('.copy-btn').on('click', function () {
  const preText = $(this).siblings('pre').text();

  if (navigator.clipboard) {
    navigator.clipboard.writeText(preText).then(() => {
      alert('Copied to clipboard!');
    }).catch(err => {
      alert('Copy failed!');
      console.error(err);
    });
  } else {
    // fallback
    const tempInput = $('<textarea>');
    $('body').append(tempInput);
    tempInput.val(preText).select();
    document.execCommand('copy');
    tempInput.remove();
    alert('Copied to clipboard!');
  }
});
