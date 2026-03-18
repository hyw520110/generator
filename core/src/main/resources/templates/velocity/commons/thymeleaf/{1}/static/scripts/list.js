$(function(){
    var searchStr = localStorage.getItem("searchStr");
    if(searchStr && searchStr.length > 0){
        searchHide(searchStr);
    }
    var colStr = localStorage.getItem("colStr");
    if(colStr && colStr.length > 0){
        colHide(colStr);
    }
    var sortStr = localStorage.getItem("sortStr");
    if(sortStr && sortStr.length > 0){
        sortchange(sortStr);
    }
	$("#pageInfo a").click(page);
    $("#table th").click(tableFun);
    $("#searchList").hover(function(){
        $("#searchList").addClass("open");
    },function(){
        $("#searchList").removeClass("open");
    });
    $("#searchList li").click(function(){
        $("#searchList").removeClass("open");
        if($(this).hasClass("columnVisibility")){
            $("form input[name=" + $(this).attr("data-name") + "]").parent().parent().show();
            $(this).removeClass("columnVisibility");
        }else{
            $("form input[name=" + $(this).attr("data-name") + "]").val("");
            $("form input[name=" + $(this).attr("data-name") + "]").parent().parent().hide();
            $(this).addClass("columnVisibility");
        }
        searchHide();
    });
    $("#hideList").hover(function(){
        $("#hideList").addClass("open");
    },function(){
        $("#hideList").removeClass("open");
    });
    $("#hideList li").click(function(){
        $("#hideList").removeClass("open");
        if($(this).hasClass("columnVisibility")){
            $("#table th[data-name=" + $(this).attr("data-name") + "]").show();
            $("#table td[data-name=" + $(this).attr("data-name") + "]").show();
            $(this).removeClass("columnVisibility");
        }else{
            $("#table th[data-name=" + $(this).attr("data-name") + "]").hide();
            $("#table td[data-name=" + $(this).attr("data-name") + "]").hide();
            $(this).addClass("columnVisibility");
        }
        colHide();
    });
    $("#sortList").hover(function(){
        $("#sortList").addClass("open");
    },function(){
        $("#sortList").removeClass("open");
    });
    $("#sortList li a").click(function(){
        $("#sortList").removeClass("open");
        var n = $(this).text().lastIndexOf("/");
        if($(this).text().substr(n + 1,4) === "当前数据"){
            $(this).text($(this).text().substring(0,n + 1) + "所有数据");
            $("#table thead th[data-name=" + $(this).attr("data-name") + "]").attr("data-type","1");
        }else{
            $(this).text($(this).text().substring(0,n + 1) + "当前数据");
            $("#table thead th[data-name=" + $(this).attr("data-name") + "]").attr("data-type","0");
        }
        sortchange();
    });
    $("a[name=layerBtn]").click(function(){
        layer.open({
          type: 2,
          title: $(this).attr("data-title"),
          shadeClose: true,
          shade: 0.6,
          maxmin: true, //开启最大化最小化按钮
          area: ['893px', '600px'],
          content: $(this).attr("href")
        });
        return false;
    });
    $("#pageRowsList .ui-selectmenu-button").click(function(){
        $("#pageRowsList .ui-selectmenu-menu").toggle();
    })
    if(localStorage.getItem("pageRows") && localStorage.getItem("pageRows") != $("#pageRowsList .ui-selectmenu-text").text()){
        var href=$("#pageRowsList a").attr("href") + "&pageRows=" + localStorage.getItem("pageRows");
        $("form input[type='text'][value!='']").each(function(){
            href+="&"+this.name+"="+this.value;
        });
        location.href = href;
    }
    $("#pageRowsList li").click(function(){
        $("#pageRowsList li").removeClass("ui-state-focus");
        $(this).addClass("ui-state-focus");
        $("#pageRowsList .ui-selectmenu-menu").hide();
        $("#pageRowsList .ui-selectmenu-text").text($(this).text());
        var href=$("#pageRowsList a").attr("href") + "&pageRows=" + $(this).text();
        localStorage.setItem("pageRows",$(this).text());
        $("form input[type='text'][value!='']").each(function(){
            href+="&"+this.name+"="+this.value;
        });
        location.href = href;
    });
    $("#table td .yg-checkbox").click(function(){
        if($(this).hasClass("checked")){
            $(this).removeClass("checked");
        }else{
            $(this).addClass("checked");
        }
    });
    $("#table th .yg-checkbox").click(function(){
        if($(this).hasClass("checked")){
            $(this).removeClass("checked");
            $("#table td .yg-checkbox").removeClass("checked");
        }else{
            $(this).addClass("checked");
            $("#table td .yg-checkbox").addClass("checked");
        }
    });
    $("#sortAlert td .yg-checkbox").click(function(){
        if($(this).hasClass("checked")){
            $(this).removeClass("checked");
        }else{
            $(this).addClass("checked");
        }
    });
    $("#sortSearch").click(function(){
        $('#sortAlertBg').show();
        layer.open({
          type: 1,
          shade: false,
          content: $('#sortAlert'), //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响,
          cancel:function(){
                $('#sortAlertBg').hide();
          }
        });
    });
})
function searchHide(str){
    if(str && str.length > 0){
        var arr = str.split(",");
        for(var i = arr.length - 1;i >= 0;i--){
            if(arr[i].split("=")[1] == 1){
                $("form input[name=" + arr[i].split("=")[0] + "]").parent().parent().show();
            }else{
                $("form input[name=" + arr[i].split("=")[0] + "]").parent().parent().hide();
                $("#searchList li[data-name=" + arr[i].split("=")[0] + "]").addClass("columnVisibility");
            }
            $("form input[name=" + arr[i].split("=")[0] + "]").val("");

        }
    }else{
        var list = $("#searchList li");
        var searchStr = "";
        for(var i = list.length - 1;i >= 0;i--){
            searchStr += list.eq(i).attr("data-name") + "=" + (list.eq(i).hasClass("columnVisibility") ? 0 : 1) + ",";
        }
        if(searchStr.length > 0){
            searchStr = searchStr.substr(0,searchStr.length - 1);
        }
        localStorage.setItem("searchStr",searchStr);
    }
}
function colHide(str){
    if(str && str.length > 0){
        var arr = str.split(",");
        for(var i = arr.length - 1;i >= 0;i--){
            if(arr[i].split("=")[1] == 1){
                $("#table th[data-name=" + arr[i].split("=")[0] + "]").show();
                $("#table td[data-name=" + arr[i].split("=")[0] + "]").show();
            }else{
                $("#table th[data-name=" + arr[i].split("=")[0] + "]").hide();
                $("#table td[data-name=" + arr[i].split("=")[0] + "]").hide();
                $("#hideList li[data-name=" + arr[i].split("=")[0] + "]").addClass("columnVisibility");
            }
        }
    }else{
        var list = $("#hideList li");
        var colStr = "";
        for(var i = list.length - 1;i >= 0;i--){
            colStr += list.eq(i).attr("data-name") + "=" + (list.eq(i).hasClass("columnVisibility") ? 0 : 1) + ",";
        }
        if(colStr.length > 0){
            colStr = colStr.substr(0,colStr.length - 1);
        }
        localStorage.setItem("colStr",colStr);
    }
}
function sortchange(str){
    if(str && str.length > 0){
        var arr = str.split(",");
        for(var i = arr.length - 1;i >= 0;i--){
            var obj = $("#sortList li[data-name=" + arr[i].split("=")[0] + "]").find("a");
            var n =  obj.text().lastIndexOf("/");
            obj.text(obj.text().substring(0,n + 1) + arr[i].split("=")[1]);
        }
    }else{
        var list = $("#sortList li");
        var sortStr = "";
        for(var i = list.length - 1;i >= 0;i--){
            var n =  list.eq(i).text().lastIndexOf("/");
            sortStr += list.eq(i).attr("data-name") + "=" + list.eq(i).find("a").text().substr(n + 1,4) + ",";
        }
        if(sortStr.length > 0){
            sortStr = sortStr.substr(0,sortStr.length - 1);
        }
        localStorage.setItem("sortStr",sortStr);
    }
}
function page(){
	var href=$(this).attr("href");
	$("form input[type='text'][value!='']").each(function(){
		href+="&"+this.name+"="+this.value;
	});
	$(this).attr("href",href);
}
function tableFun(){
    var self = this;
    if($(this).attr("data-name") && $(this).attr("data-name") !== ""){
        var list = $("#table tbody tr");
        if($(this).hasClass("sorting_asc")){
            list.sort(function(a,b){
                if(typeof parseFloat($(a).find("td[data-name=" + $(self).attr("data-name") + "]").text()) === "number" && typeof parseFloat($(b).find("td[data-name=" + $(self).attr("data-name") + "]").text()) === "number"){
                    return parseFloat($(a).find("td[data-name=" + $(self).attr("data-name") + "]").text()) > parseFloat($(b).find("td[data-name=" + $(self).attr("data-name") + "]").text());
                }else{
                    return $(a).find("td[data-name=" + $(self).attr("data-name") + "]").text() > $(b).find("td[data-name=" + $(self).attr("data-name") + "]").text();
                }
            });
            $(this).addClass("sorting_desc");
            $(this).removeClass("sorting_asc");
        }else{
            list.sort(function(a,b){
                if(typeof parseFloat($(a).find("td[data-name=" + $(self).attr("data-name") + "]").text()) === "number" && typeof parseFloat($(b).find("td[data-name=" + $(self).attr("data-name") + "]").text()) === "number"){
                    return parseFloat($(a).find("td[data-name=" + $(self).attr("data-name") + "]").text()) < parseFloat($(b).find("td[data-name=" + $(self).attr("data-name") + "]").text());
                }else{
                    return $(a).find("td[data-name=" + $(self).attr("data-name") + "]").text() < $(b).find("td[data-name=" + $(self).attr("data-name") + "]").text();
                }
            });
            $(this).removeClass("sorting_desc");
            $(this).addClass("sorting_asc");
        }
        $("#table thead th").each(function(){
            if(this.getAttribute("data-name") && this.getAttribute("data-name") !== self.getAttribute("data-name")){
                $(this).removeClass("sorting_desc");
                $(this).removeClass("sorting_asc");
            }
        });
        $("#table tbody").html(list);
    }
}
function delFun(obj){
    layer.confirm('您是否确认删除?', {
      btn: ['确定','取消'] //按钮
    }, function(){
        location.href = obj.href;
    });
    return false;
}
