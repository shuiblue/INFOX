<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
  <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>

<script type="text/javascript">

//window.onload = refreshPage();

function refreshPage(){
  var table_height = $("#cluster").height();
  // console.log(table_height);
  var container = document.getElementById("js-repo-pjax-container");
  container.style.top= table_height-80 +"px";
  container.style.position="relative";
  var tabs = document.getElementsByClassName("tabnav-tab");
  tabs[0].className = "tabnav-tab";
  tabs[1].className= "tabnav-tab selected";

  var commits_bucket = document.getElementById("commits_bucket");
  var files_bucket = document.getElementById("files_bucket");
  commits_bucket.style.display="none";
  files_bucket.style.display="block";
  $(".pagehead").remove();
}

function hide_non_cluster_rows() {
  
  var table_rows = document.getElementsByTagName('tr');
  var rows_to_be_hidden = [];
  for(i=0; i<table_rows.length; i++){
    var tds = table_rows[i].getElementsByTagName('td');
    if(tds.length==3){
      
      if((tds[0].getAttribute("class") == "blob-num blob-num-addition empty-cell" || 
        tds[0].getAttribute("class") == "blob-num blob-num-deletion js-linkable-line-number" || tds[0].getAttribute("class") == "blob-num blob-num-context js-linkable-line-number") && !(tds[1].getAttribute("class").startsWith("infox_"))){
        rows_to_be_hidden.push(table_rows[i]);
        // console.log("row " + i + " has only 3 tds and is a row we want to hide" );
      }
  }
  }

  for(r=0; r<rows_to_be_hidden.length; r++){
    rows_to_be_hidden[r].style.display = rows_to_be_hidden[r].style.display == "none" ? "table-row" : "none";
  }
  var btn = document.getElementById("btn_hide_non_cluster_rows")
  if(btn.innerHTML == "Show hidden non cluster code"){
    btn.innerHTML = "Hide non cluster code"
  } 
  else {
    btn.innerHTML = "Show hidden code"
  }
    
}

function hide_cluster_rows(cluster_id) {
  
        var rows = document.getElementsByClassName(cluster_id);
        for(r=0; r<rows.length;r++){
        rows[r].parentElement.style.display = rows[r].parentElement.style.display == "none" ? "table-row" : "none";}
  
}
function go_to_row(row) {
if ($(row).length){
        var table_height = $("#cluster").height();
        $(window).scrollTop( $(row).offset().top - table_height - 20);
    }
}

var cluster_rows = {};
var current_index = {};
var w = $(window);
function next_in_cluster(cluster_id) {
  console.log(current_index);
  var c = cluster_rows[cluster_id];
  if(c == null ){
    var cluster = [];
    var rows_in_cluster = document.getElementsByClassName(cluster_id);
    cluster.push(rows_in_cluster[0]);
    for (row=1; row < rows_in_cluster.length; row++){
      if(parseInt(rows_in_cluster[row].getAttribute('data-line-number')) == (parseInt(rows_in_cluster[row-1].getAttribute('data-line-number')) + 1)) {
        // consecutive_rows.push(rows_in_cluster[row]);
      }
      else {
        cluster.push(rows_in_cluster[row]);
      }
    }
    cluster_rows[cluster_id] = cluster;
  }

  if(current_index[cluster_id] == null) {
    current_index[cluster_id] = 0;
  }

  else {

    if(cluster_rows[cluster_id].length == 1){
      current_index[cluster_id] = 0;
    }
    else if(current_index[cluster_id] < cluster_rows[cluster_id].length){
      console.log(current_index[cluster_id]);
      current_index[cluster_id] = (parseInt(current_index[cluster_id]) + 1);
    }
  }
  var row = cluster_rows[cluster_id];
  var show_row = row[parseInt(current_index[cluster_id])];
  go_to_row(document.getElementById(show_row.getAttribute("id")));
  
}


function prev_in_cluster(cluster_id) {

  if(current_index[cluster_id] == null) {
    
  }
  else {
    if(current_index[cluster_id] > 0){
      current_index[cluster_id] = current_index[cluster_id] - 1;
    }
  }

  go_to_row(document.getElementById(cluster_rows[cluster_id][current_index[cluster_id]].getAttribute("id")));
}
</script>
