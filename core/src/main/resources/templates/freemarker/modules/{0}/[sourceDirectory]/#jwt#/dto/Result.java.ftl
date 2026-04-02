package ${dtoPackage!};

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 接口返回数据格式
 * 
 * @author heyw
 * @copyright: hyw Copyright (c) 2017-2020 All Rights Reserved.
 */
@Schema(name = "Result", description = "接口返回对象")
public class Result<T> implements Serializable {
	private static final long serialVersionUID = -862936613454307838L;
	@Schema(name = "status", description = "状态码")
	private int status;
	@Schema(name = "message", description = "消息")
	private String message;
	@Schema(name = "data", description = "数据")
	private T data;
	@Schema(name = "timestamp", description = "时间戳")
	private long timestamp;

	public Result() {
		this.status = StatusCode.SUCCESS.getCode();
		this.message = StatusCode.SUCCESS.getDesc();
		this.timestamp = System.currentTimeMillis();
	}

	public Result(int status, String message) {
		this.status = status;
		this.message = message;
		this.timestamp = System.currentTimeMillis();
	}

	public Result(T data) {
		this.status = StatusCode.SUCCESS.getCode();
		this.message = StatusCode.SUCCESS.getDesc();
		this.data = data;
		this.timestamp = System.currentTimeMillis();
	}

	public static <T> Result<T> ok() {
		return new Result<>();
	}

	public static Result<Object> message(String message) {
		return ok(message, null);
	}

	public static <T> Result<T> ok(T data) {
		return ok("ok", data);
	}

	public static <T> Result<T> ok(String message, T data) {
		Result<T> r = new Result<T>();
		r.setMessage(message);
		r.setData(data);
		return r;
	}

	public static <T> Result<T> error() {
		return error(StatusCode.SYSTEM_ERROR.getCode(), StatusCode.SYSTEM_ERROR.getDesc());
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