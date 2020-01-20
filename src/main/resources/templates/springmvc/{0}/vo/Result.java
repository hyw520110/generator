package ${voPackage};

import java.io.Serializable;

import com.big.box.demo.api.vo.Result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

 
#set( $comment='接口返回数据格式' )
#parse('/templates/commons/comment.vm')
@ApiModel(value="接口返回对象", description="接口返回对象")
public class Result<T> implements Serializable {
	private static final long serialVersionUID = -862936613454307838L;
	public static final Integer SC_OK_200 = 200;
	public static final String OK_MESSAGE = "操作成功";
	public static final Integer SC_SERVER_ERROR_500 = 500;
	public static final String SC_SERVER_ERROR_MESSAGE = "发生错误";
	private int status;
	private String message;
	private T data;
	private long timestamp;

	public Result() {
		this.status = SC_OK_200;
		this.message = OK_MESSAGE;
		this.timestamp = System.currentTimeMillis();
	}

	public Result(int status, String message) {
		this.status = status;
		this.message = message;
		this.timestamp = System.currentTimeMillis();
	}

	public Result(T data) {
		this();
		this.data = data;
		this.timestamp = System.currentTimeMillis();
	}

	public static <T> Result<T> ok() {
		return new Result<>();
	}

	public static Result<Object> ok(String message) {
		return ok(message, null);
	}

	public static Result<Object> ok(Object data) {
		return ok(null, data);
	}

	public static Result<Object> ok(String message, Object data) {
		Result<Object> r = new Result<Object>();
		r.setMessage(message);
		r.setData(data);
		return r;
	}

	public static <T> Result<T> error() {
		return error(SC_SERVER_ERROR_500, SC_SERVER_ERROR_MESSAGE);
	}

	public static <T> Result<T> error(int code, String message) {
		return new Result<>(code, message);
	}

	public int getStatus() {
		return this.status;
	}

	public String getMessage() {
		return this.message;
	}

	public T getData() {
		return this.data;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setData(T data) {
		this.data = data;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}