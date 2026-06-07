package ${dtoPackage};

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageResult", description = "分页返回对象")
public class PageResult<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<T> records = Collections.emptyList();
	private long current;
	private long size;
	private long total;
	private long pages;

	public PageResult() {
	}

	public PageResult(List<T> records, long current, long size, long total) {
		this.records = records == null ? Collections.emptyList() : records;
		this.current = current;
		this.size = size;
		this.total = total;
		this.pages = size > 0 ? (total + size - 1) / size : 0;
	}

	public static <T> PageResult<T> of(List<T> records, long current, long size, long total) {
		return new PageResult<>(records, current, size, total);
	}

	public List<T> getRecords() {
		return records;
	}

	public void setRecords(List<T> records) {
		this.records = records == null ? Collections.emptyList() : records;
	}

	public long getCurrent() {
		return current;
	}

	public void setCurrent(long current) {
		this.current = current;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
		this.pages = size > 0 ? (total + size - 1) / size : 0;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
		this.pages = size > 0 ? (total + size - 1) / size : 0;
	}

	public long getPages() {
		return pages;
	}

	public void setPages(long pages) {
		this.pages = pages;
	}
}
