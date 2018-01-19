$(function(){
	$(".detail").click(detail);
	$("#search").keyup(search);
})

function detail(){
	var obj=$(this).text($.trim($(this).text())=="+"?"-":"+");
	$(this).parent().next().toggle();
}

function search(){
	var val=$.trim($(this).val());
	if(val){
		$("#tables>tbody>tr").hide().filter("tr:not('.detail-row'):contains('"+val+"')").show().css("font-weight","bold");
		return ;
	} 
	$("#tables>tbody>tr:not('.detail-row')").css("font-weight","normal").show();
}