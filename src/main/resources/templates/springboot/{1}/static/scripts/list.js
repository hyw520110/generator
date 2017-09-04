$(function(){
	$("#pageInfo a").click(page);
})

function page(){
	var href=$(this).attr("href");
	$("form input[type='text'][value!='']").each(function(){
		href+="&"+this.name+"="+this.value;
	});
	$(this).attr("href",href);
}