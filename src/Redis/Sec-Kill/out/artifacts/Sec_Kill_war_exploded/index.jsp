<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>电商秒杀Redis实现</title>
</head>
<body>
    <h1>电商秒杀Redis实现</h1>

    <%--doseckill的映射要在web.xml配置，映射到com.hypocrite30.RedisSecKill.SecKillServlet--%>
    <form id="msform" action="${pageContext.request.contextPath}/doseckill" enctype="application/x-www-form-urlencoded">
        <input type="hidden" id="prodid" name="prodid" value="0101"><%--商品id为0101--%>
        <input type="button" id="miaosha_btn" name="seckill_btn" value="秒杀点我"/>
    </form>

</body>
<script type="text/javascript" src="${pageContext.request.contextPath}/script/jquery/jquery-3.1.0.js"></script>
<script type="text/javascript">
    $(function () {
        $("#miaosha_btn").click(function () {
            var url = $("#msform").attr("action");
            $.post(url, $("#msform").serialize(), function (data) {
                if (data == "false") {
                    alert("抢光了");
                    $("#miaosha_btn").attr("disabled", true);
                }
            });
        });
    });
</script>
</html>