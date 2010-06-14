window.addEventListener('load', function() {
  var current;
  function navigate(dst) {
    if (dst) {
      if (current) {
        current.style.display = 'none';
      } else {
        for (var i=document.body.firstElementChild; i; i=i.nextElementSibling) {
          i.style.display = 'none';
        }
      }
      current = dst;
      if (dst.id) {
        window.location.hash = '#' + dst.id;
      }
      dst.style.display = 'block';
    }
  }
  function getFirst() { return document.body.firstElementChild; }
  function getLast() { return document.body.lastElementChild; }
  function getNext() {
    return current ? current.nextElementSibling : getFirst();
  }
  function getPrevious() {
    return current ? current.previousElementSibling : getLast();
  }
  navigate(document.getElementById(window.location.hash.substring(1)));
  window.addEventListener('keydown', function (e) {
    switch (e.keyCode) {
    case 34:  // PgUp
    case 39:  // right arrow
      navigate(getNext());
      break;
    case 33:  // PgDn
    case 37:  // left arrow
      navigate(getPrevious());
      break;
    case 36:  // Home
      navigate(getFirst());
      break;
    case 35:  // End
      navigate(getLast());
      break;
    }
  }, false);
}, false);
