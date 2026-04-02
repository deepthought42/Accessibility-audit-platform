const $  = document.querySelector.bind(document);
var element_ref = null;

/**
 * converts value between 0 and 255 to hex value
 */
function componentToHex(c) {
  const hex = c.toString(16);
  return hex.length == 1 ? "0" + hex : hex;
}

/**
 * Converts rgb value to hex
 * @param {} r 
 * @param {*} g 
 * @param {*} b 
 * @returns 
 */
function rgbToHex(rgb_string) {
  var tmp_color_str = rgb_string.replace(")", "");
  tmp_color_str = tmp_color_str.replace("rgba(", "");
  tmp_color_str = tmp_color_str.replace("rgb(", "");
  tmp_color_str = tmp_color_str.replaceAll(" ", "");
  var rgb = tmp_color_str.split(",");
  
  var red = parseInt(rgb[0], 10);
  var green = parseInt(rgb[1], 10);
  var blue = parseInt(rgb[2], 10);
  
  return "#" + componentToHex(red) + componentToHex(green) + componentToHex(blue);
}

function isScript(element){
  return element.tagName.toLowerCase() === "script";
}

function isHidden(element){
  var parent = element;
  //check if element is hidden
  while(parent != undefined && parent != null){
    if(getComputedStyle(parent).display === 'none' || getComputedStyle(parent).visibility === 'hidden' || getComputedStyle(parent).maxHeight == "0px"){
      return true;
    }
    parent = parent.parentElement;
  }

  return false;
}

function sRGBtoLin(colorChannel) {
  // Send this function a decimal sRGB gamma encoded color value
  // between 0.0 and 1.0, and it returns a linearized value.

  if ( colorChannel <= 0.04045 ) {
      return colorChannel / 12.92;
  } else {
      return Math.pow((( colorChannel + 0.055)/1.055),2.4);
  }
}

/**
	 * Calculates luminosity from rgb color
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
 function calculatePercievedLightness(red, green, blue) {
  //calculate luminosity
  //For the sRGB colorspace, the relative luminance of a color is defined as
  //where R, G and B are defined as:
  
  var RsRGB = red/255.0;
  var GsRGB = green/255.0;
  var BsRGB = blue/255.0;

  var R = sRGBtoLin(RsRGB);
  var G = sRGBtoLin(GsRGB);
  var B = sRGBtoLin(BsRGB);
  
  return 0.2126 * R + 0.7152 * G + 0.0722 * B ;
}

function doesExistInArray(text_array, lines){
  for(var i=0; i<text_array.length; i++){
    if(text_array[i].includes(lines)){
      return true;
    }
  }

  return false;
}

/**
 * Retrieves set of elements that own text. Text ownership is defined as having text within an
 *   an element that belongs to that element and not any of it's children. 
 */
function getElementsThatOwnText(){
  var elements = document.body.getElementsByTagName("*");
  var text_elements = [];
  var string_arr = [];
  
  for(var i=0; i < elements.length; i++) {
    var lines = elements[i].textContent.split(/\r?\n/);
    //remove lines with &npsp;
    var scrubbed_lines = [];
    for(var j=0; j < lines.length; j++){
      if(lines[j].trim().length){
        scrubbed_lines.push(lines[j]);
      }
    }

    if(!isScript(elements[i]) && scrubbed_lines.length == 1){
      if(!doesExistInArray(string_arr, scrubbed_lines)){
        text_elements.push(elements[i]);
        string_arr.push(elements[i].textContent.trim());
      }
    }
  }

  return text_elements;
}

function getButtonElements(){
  var button_elements = document.body.getElementsByTagName("button");
  var button_class_elements = document.body.getElementsByClassName("button");

  var elements = [];

  if(button_elements != undefined){ 
    for(var i=0; i < button_elements.length; i++) {
      if(!isHidden(button_elements[i]) && getComputedStyle(button_elements[i]).backgroundColor != "rgba(0, 0, 0, 0)"){
        elements.push(button_elements[i]);
      }
    }
  }

  if(button_class_elements != undefined){ 
    for(var i=0; i < button_class_elements.length; i++) {
      if(!isHidden(button_class_elements[i]) && getComputedStyle(button_class_elements[i]).backgroundColor != "rgba(0, 0, 0, 0)"){
        elements.push(button_class_elements[i]);
      }
    }
  }

  return elements;
}

/**
 * Convert rgba to rgb
 * 
 * @param {*} RGB_background 
 * @param {*} RGBA_color 
 * @returns 
 */
function rgba2rgb(RGB_background, RGBA_color)
{
    var alpha = RGBA_color.a;

    return new Color(
        (1 - alpha) * RGB_background.r + alpha * RGBA_color.r,
        (1 - alpha) * RGB_background.g + alpha * RGBA_color.g,
        (1 - alpha) * RGB_background.b + alpha * RGBA_color.b
    );
}

/**
 * Computes the contrast between 2 colors
 * @param {*} color1 
 * @param {*} color2 
 */
function calculateContrast(color1, color2){
  /* color1 rgb deconstruction */
  
  var tmp_color_str = color1.replace(")", "");
  tmp_color_str = tmp_color_str.replace("rgba(", "");
  tmp_color_str = tmp_color_str.replace("rgb(", "");
  tmp_color_str = tmp_color_str.replaceAll(" ", "");
  var rgba1 = tmp_color_str.split(",");
  
  var red1 = rgba1[0];
  var green1 = rgba1[1];
  var blue1 = rgba1[2];
  if(color1.includes("rgba(")){
    var transparency1 = rgba1[3];

    if(transparency1 > 0){
      return 21.0;
    }
    else {
      red1 = 255;
      green1 = 255;
      blue1 = 255;
    }  
  }

  /* color1 rgb deconstruction */
  var tmp_color_str2 = color2.replace(")", "");
  tmp_color_str2 = tmp_color_str2.replace("rgba(", "");
  tmp_color_str2 = tmp_color_str2.replace("rgb(", "");
  tmp_color_str2 = tmp_color_str2.replaceAll(" ", "");
  var rgba2 = tmp_color_str2.split(",");
  
  var red2 = rgba2[0];
  var green2 = rgba2[1];
  var blue2 = rgba2[2];
  if(color2.includes("rgba(")){
    var transparency2 = rgba2[3];
    if(transparency2 > 0){
      return 21.0;
    }
    else {
      red2 = 255;
      green2 = 255;
      blue2 = 255;
    }
  }

  var color_1_luminosity = calculatePercievedLightness(red1, green1, blue1);
  var color_2_luminosity = calculatePercievedLightness(red2, green2, blue2);
  
  var max_luminosity = 0.0;
  var min_luminosity = 0.0;

  if(color_1_luminosity > color_2_luminosity) {
    min_luminosity = color_2_luminosity;
    max_luminosity = color_1_luminosity;
  }
  else {
    min_luminosity = color_1_luminosity;
    max_luminosity = color_2_luminosity;
  }

  return (max_luminosity + 0.05) / (min_luminosity + 0.05);
}

/**
 * Checks if contrast meets AA compliance given font size and weight
 * 
 * @param {*} contrast 
 * @param {*} font_size 
 * @param {*} font_weight 
 */
function getIsAACompliant(contrast, font_size, font_weight){
  if(font_size >= 18.0 && contrast >= 3.0){
    return true;
  }
  else if(font_size >= 14.0 && font_weight >= 700 && contrast >= 3.0){
    return true;
  }
  else if((font_size < 18.0 && contrast >= 4.5)){
    return true;
  }

  return false;
}

/**
 * Checks if contrast meets AAA compliance given font size and weight
 * 
 * @param {*} contrast 
 * @param {*} font_size 
 * @param {*} font_weight 
 */
function getIsAAACompliant(contrast, font_size, font_weight){
  if(font_size >= 18.0 && contrast >= 4.5){
    return true;
  }
  else if(font_size >= 14.0 && font_weight >= 700 && contrast >= 4.5){
    return true;
  }
  else if(font_size < 18.0 && contrast >= 7.0){
    return true;
  }

  return false;
}

/**
 * Checks if contrast meets AA compliance given font size and weight
 * 
 * @param {*} contrast 
 * @param {*} font_size 
 * @param {*} font_weight 
 */
 function getIsNonTextAACompliant(contrast){
  if(contrast >= 3.0){
    return true;
  }

  return false;
}

/**
 * Checks if contrast meets AAA compliance given font size and weight
 * 
 * @param {*} contrast 
 * @param {*} font_size 
 * @param {*} font_weight 
 */
function getIsNonTextAAACompliant(contrast){
  if(contrast >= 3.0){
    return true;
  }

  return false;
}

function getTextSizeString(font_size, font_weight){
  if(font_size >= 18 || (font_size >= 14 && font_weight >= 700)){
    return "Large Text";
  }

  return "Small Text";
}

function getBackgroundColor(element){
  parent = element;
  while(parent != undefined && parent != null){
    if( getComputedStyle(parent).backgroundColor != "rgba(0, 0, 0, 0)"){
      return getComputedStyle(parent).backgroundColor;
    }
    parent = parent.parentElement;
  }

  return getComputedStyle(element).backgroundColor;
}
/**
 * Checks all text elements for color contrast issues
 * @param {*} event 
 */
function reviewTextContrast()
{
  var contrast_issues = [];

  // GET ALL ELEMENTS THAT OWN TEXT
  var elements = getElementsThatOwnText()
  var idx = 0;
  for(var i=0; i< elements.length; i++){
    var element = elements[i];
    var element_style = getComputedStyle(element);
    //  GET TEXT COLOR
    var text_color = element_style.color;
    //  GET BACKGROUND COLOR
    var bg_color = getBackgroundColor(element);
    //  RETRIEVE FONT SIZE
    var font_size = parseInt(element_style.fontSize, 10) * (72.0 / 96.0);
    //  RETRIEVE FONT WEIGHT
    var font_weight = element_style.fontWeight;

    
    //  CALCULATE CONTRAST VALUE
    var contrast = calculateContrast(text_color, bg_color);
    contrast = Math.round((contrast + Number.EPSILON) * 100) / 100

    //  CHECK IF ELEMENT IS AA COMPLIANT
    var is_aa_compliant = getIsAACompliant(contrast, font_size, font_weight);
    //  CHECK IF ELEMENT IS AAA COMPLIANT
    var is_aaa_compliant = getIsAAACompliant(contrast, font_size, font_weight);
    
    

    //  IF NOT AA OR AAA COMPLAINT THEN ASSIGN CLASS TO EACH ELEMENT WITH ELEMENT ID SUCH THAT IT HAS THE FORMAT "TEXT_ELEMENT_$INDEX"
    if(!is_aa_compliant || !is_aaa_compliant){
      element.classList.add("text_element_"+idx);
      //  CREATE NEW CONTRAST ISSUE

      let issue = new ContrastIssue(rgbToHex(text_color), rgbToHex(bg_color), contrast, getTextSizeString(font_size, font_weight), "text_element_"+idx, is_aa_compliant, is_aaa_compliant);
      //  ADD CONTRAST ISSUE TO ISSUE LIST
      contrast_issues.push(issue);
    }
    idx = idx + 1;  
  }

  return contrast_issues;
}

function findParentWithDifferentBackground(element){
  var parent = element.parentElement;
  var element_bg_color = getComputedStyle(element).backgroundColor;
  while(parent != undefined && parent != null){
    if((getComputedStyle(parent).backgroundColor != element_bg_color && getComputedStyle(parent).backgroundColor != "rgba(0, 0, 0, 0)")){
      return getComputedStyle(parent).backgroundColor;
    }
    parent = parent.parentElement;
  }

  if(getComputedStyle(element).backgroundColor == "rgba(0, 0, 0, 0)"){
    return "rgb(255, 255, 255)";
  }

  return getComputedStyle(element).backgroundColor;
}

/**
 * Checks all non text elements for color contrast issues
 * @param {*} event 
 */
 function reviewNonTextContrast()
 {
  var contrast_issues = [];

  // GET ALL ELEMENTS THAT OWN TEXT
  var elements = getButtonElements();
  var idx = 0;
  for(var i=0; i< elements.length; i++){
    var element = elements[i];
    //  GET TEXT COLOR
    var element_color = getComputedStyle(element).backgroundColor;
    //  GET BACKGROUND COLOR
    var bg_color = findParentWithDifferentBackground(element);

    //  CALCULATE CONTRAST VALUE
    var contrast = calculateContrast(element_color, bg_color);
    //  CHECK IF ELEMENT IS AA COMPLIANT
    var is_aa_compliant = getIsNonTextAACompliant(contrast);
    //  CHECK IF ELEMENT IS AAA COMPLIANT
    var is_aaa_compliant = getIsNonTextAAACompliant(contrast);

    //  IF NOT AA OR AAA COMPLAINT THEN ASSIGN CLASS TO EACH ELEMENT WITH ELEMENT ID SUCH THAT IT HAS THE FORMAT "TEXT_ELEMENT_$INDEX"
    if(!is_aa_compliant || !is_aaa_compliant){
      element.classList.add("non_text_"+idx);
      
      //  CREATE NEW CONTRAST ISSUE
      contrast = Math.round((contrast + Number.EPSILON) * 100) / 100

      let issue = new ContrastIssue(rgbToHex(element_color), rgbToHex(bg_color), contrast, "non-text", "non_text_"+idx, is_aa_compliant, is_aaa_compliant);
      //  ADD CONTRAST ISSUE TO ISSUE LIST
      contrast_issues.push(issue);
    }
    idx = idx + 1;  
  }

  return contrast_issues;
 }

//window.addEventListener("message", receiveMessage, false);

// Listen for messages from the popup.
chrome.runtime.onMessage.addListener((msg, sender, response) => {
  if(msg.method == "analyzeContrast"){
    var s = document.documentElement.outerHTML; 

    var text_contrast_issues = reviewTextContrast();
    var non_text_contrast_issues = reviewNonTextContrast();
    let contrast_issues = text_contrast_issues;

    for(var i=0; i<non_text_contrast_issues.length; i++){
      contrast_issues.push(non_text_contrast_issues[i]);
    }
    response(contrast_issues);
  }
  else if(msg.method == "viewIssue"){
    if(element_ref != null){
      element_ref.style.border = "";
    }
    //find element with the class in data
    element_ref = $("."+msg.data);
    element_ref.scrollIntoView();
    var border_color = "red";
    if(getComputedStyle(element_ref).backgroundColor == "(255,0,0)"){
      border_color = "blue";
    }
    element_ref.style.border = "4px solid red";
    response(true);
  }
});

class ContrastIssue {
  constructor(foreground, background, contrast, type, element_ref, is_aa_compliant, is_aaa_compliant){
    this.foreground_color = foreground;
    this.background_color = background;
    this.contrast = contrast;
    this.type = type;
    this.element_ref = element_ref;
    this.is_aa_compliant = is_aa_compliant;
    this.is_aaa_compliant = is_aaa_compliant;
  }
}