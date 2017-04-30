var ImageResizer = function() {};

ImageResizer.prototype.resize = function(options, success, fail) {
  cordova.exec(function(uri) {
    success(uri);
  }, function(e) {
    fail(e);
  }, "ImageResizer", "resize", [options]);
};

/**
 * Get an image width and height
 * @param success success callback, will receive the data sent from the native plugin
 * @param fail error callback, will receive an error string describing what went wrong
 * @param imageData The image data, either base64 or local url
 * @param options extra options -
 *              imageDataType : the data type (IMAGE_DATA_TYPE_URL/IMAGE_DATA_TYPE_BASE64) - defaults to URL
 * @returns JSON Object with the following parameters:
 *              height : height of the image
 *              width: width of the image
 */
ImageResizer.prototype.size = function(options, success, fail) {
  if (!options) {
    options = {};
  }

  var params = {
    data: options.imageData,
    imageDataType: options.imageDataType ? options.imageDataType : ImageResizer.IMAGE_DATA_TYPE_URL
  };

  cordova.exec(function(data){
    success(data);
  }, function(e){fail(e)}, "ImageResizer", "size", [params]);
};

var imageResizer = new ImageResizer();
module.exports = imageResizer;
