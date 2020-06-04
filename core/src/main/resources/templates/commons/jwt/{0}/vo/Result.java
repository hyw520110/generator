package ${voPackage};

import java.io.Serializable;

import ${enumsPackage}.StatusCode;

import io.swagger.annotations.ApiModel;

/**
 * 接口返回数据格式
 * 
 * @author heyw
 * @since 2020-01-08 10:27:45
 * @copyright: hyw Copyright (c) 2017-2020 All Rights Reserved.
 */
@ApiModel(value = "接口返回对象", description = "接口返回对象")
public class Result<T> implements Serializable {
	private static final long serialVersionUID = -862936613454307838L;
	private int status;
	private String message;
	private T data;
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

	public static Result<Object> ok(String message) {
		return ok(message, null);
	}

	public static Result<Object> ok(Object data) {
		return ok("ok", data);
	}

	public static Result<Object> ok(String message, Object data) {
		Result<Object> r = new Result<Object>();
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