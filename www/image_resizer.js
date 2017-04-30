var ImageResizer = function() {};

ImageResizer.prototype.resize = function(options, success, fail) {
  cordova.exec(function(uri) {
    success(uri);
  }, function(e) {
    fail(e);
  }, "ImageResizer", "resize", [options]);
};

ImageResizer.prototype.size = function(options, success, fail) {
  cordova.exec(function(size) {
    success(size);
  }, function(e) {
    fail(e);
  }, "ImageResizer", "size", [options]);
};

var imageResizer = new ImageResizer();
module.exports = imageResizer;
