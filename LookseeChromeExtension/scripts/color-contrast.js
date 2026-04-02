//const { active } = require("auth0-chrome");
const $  = document.querySelector.bind(document);

var issue_list = [];
var active_element = null;
/**
 * Sets the values for the given issue
 * 
 * @param {*} issue 
 */
function showContrastDetails(event){
  if(active_element != null){
    active_element.classList.remove("active_issue");
  }
  active_element = this;
  active_element.classList.add("active_issue");

  $('#empty_contrast_compliance').style.display = "none";
  $('#contrast_compliance_section').style.display = "initial";

  const index = [...this.parentElement.children].indexOf(this)
  let issue = issue_list[index];

  //send message to content script with issue element_ref
  chrome.tabs.query({ active: true, currentWindow: false }, function (tabs) {

    chrome.tabs.sendMessage(tabs[0].id, { method: "viewIssue", data: issue.element_ref },
      function (res) {
        return;
      }
    )
  });


  $("#text_color").style.backgroundColor = issue.foreground_color;
  $("#text_hex_color").textContent = issue.foreground_color

  $("#bg_color").style.backgroundColor = issue.background_color;
  $("#bg_hex_color").textContent = issue.background_color

  $("#contrast").textContent = issue.contrast;

  if(issue.is_aa_compliant){
    $("#aa_success").style.display = "initial";
    $("#aa_fail").style.display = "none";
  }
  else {
    $("#aa_success").style.display = "none";
    $("#aa_fail").style.display = "initial";
  }

  if(issue.is_aaa_compliant){
    $("#aaa_success").style.display = "initial";
    $("#aaa_fail").style.display = "none";
  }
  else {
    $("#aaa_success").style.display = "none";
    $("#aaa_fail").style.display = "initial";
  }

  if(issue.type.toLowerCase() === "small text"){
    $('#non_text_element_label') == null ? "" : $('#non_text_element_label').style.display = "none";
    $('#text_element_label') == null ? "" : $('#text_element_label').style.display = "initial";
    
    $('#aa_small_text') == null ? "" : $('#aa_small_text').style.display = "initial";
    $('#aaa_small_text') == null ? "" : $('#aaa_small_text').style.display = "initial";

    $('#aa_large_text') == null ? "" : $('#aa_large_text').style.display = "none";
    $('#aaa_large_text') == null ? "" : $('#aaa_large_text').style.display = "none";
    
    $('#aa_non_text') == null ? "" : $('#aa_non_text').style.display = "none";
    $('#aaa_non_text') == null ? "" : $('#aaa_non_text').style.display = "none";
  }
  else if(issue.type.toLowerCase() === "large text"){
    $('#non_text_element_label') == null ? "" : $('#non_text_element_label').style.display = "none";
    $('#text_element_label') == null ? "" : $('#text_element_label').style.display = "initial";

    $('#aa_large_text') == null ? "" : $('#aa_large_text').style.display = "initial";
    $('#aaa_large_text') == null ? "" : $('#aaa_large_text').style.display = "initial";

    $('#aa_small_text') == null ? "" : $('#aa_small_text').style.display = "none";
    $('#aaa_small_text') == null ? "" : $('#aaa_small_text').style.display = "none";

    $('#aa_non_text') == null ? "" : $('#aa_non_text').style.display = "none";
    $('#aaa_non_text') == null ? "" : $('#aaa_non_text').style.display = "none";
  }
  else if(issue.type.toLowerCase() === "non-text"){
    $('#non_text_element_label') == null ? "" : $('#non_text_element_label').style.display = "initial";
    $('#text_element_label') == null ? "" : $('#text_element_label').style.display = "none";

    $('#aa_non_text') == null ? "" : $('#aa_non_text').style.display = "initial";
    $('#aaa_non_text') == null ? "" : $('#aaa_non_text').style.display = "initial";

    $('#aa_large_text') == null ? "" : $('#aa_large_text').style.display = "none";
    $('#aaa_large_text') == null ? "" : $('#aaa_large_text').style.display = "none";
    $('#aa_small_text') == null ? "" : $('#aa_small_text').style.display = "none";
    $('#aaa_small_text') == null ? "" : $('#aaa_small_text').style.display = "none";
    
    $('#aa_small_text').style.display = "none";
    $('#aaa_small_text').style.display = "none";
  }
}

/**
 * Constructs HTML to display the given issue as a row
 * 
 * @param {*} issue 
 * @returns 
 */
function constructContrastIssueHtml(issue){
  return "<div class='issue_row flex flex-row'>"
          + "<div class='w-30percent flex items-center text-md pl-16'>"+ issue.type +"</div>"
          + "<div class='w-60percent flex items-center justify-center text-md'>"+issue.contrast + "</div>"
          + "<input type='hidden' val='"+ issue.element_ref +"'></div>";
}

function buildContrastIssuesList(contrast_issues){
  let issue_html = "";
  if(contrast_issues === undefined){
    return "";
  }

  contrast_issues.forEach(issue => {
    issue_html += constructContrastIssueHtml(issue);

  });

  return issue_html;
}

function analyzeContrast() {    
  chrome.tabs.query({ active: true, currentWindow: false }, function (tabs) {

    chrome.tabs.sendMessage(tabs[0].id, { method: "analyzeContrast", data: "xxx" },
      function (res) {
        issue_list = res;
        $("#contrast_issues").innerHTML = buildContrastIssuesList(res);

        const issues = document.querySelectorAll('.issue_row');
        issues.forEach(issue => {
          issue.addEventListener('click', showContrastDetails);
        });
        
        /* handle the response from background here */
        if (!window.chrome.runtime.lastError) {
          // message processing code goes here
        } else {
          // error handling code goes here
        }
        return true;
      }
    )
  });
  //chrome.runtime.sendMessage({action:"analyzeContrast", data:{}});
}

document.addEventListener('DOMContentLoaded', function() {
  $("#analyzeButton").addEventListener("click", analyzeContrast);
});

