package com.aote.rs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Path("excel")
@Component
public class ExcelService {
	static Logger log = Logger.getLogger(ExcelService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;

	//��excel
	@POST
	@Path("/{count}/{cols}")
	public String exporttoexcel(String query, @PathParam("count") int count,
			@PathParam("cols") String cols) {
		log.debug("in to excel count=" + count + ", cols=" + cols);
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet();
			HSSFFont font = workbook.createFont();
			font.setColor((short) HSSFFont.COLOR_NORMAL);
			font.setBoldweight((short) HSSFFont.BOLDWEIGHT_BOLD);
			// ���ø�ʽ
			HSSFCellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setAlignment((short) HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setFont(font);
			HSSFRow row = null;
			HSSFCell cell = null;
			// ������0�� ����
			int rowNum = 0;
			row = sheet.createRow((short) rowNum);
			String[] colsStr = cols.split("\\|");
			for (int titleCol = 0; titleCol < colsStr.length; titleCol++) {
				cell = row.createCell((short) (titleCol));
				// ����������
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				// �����е��ַ���Ϊ����
				cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
				// ��������
				String[] names = colsStr[titleCol].split(":");
				// �б���,��������Ϊ����������Ϊ�ֶ�����
				if (names.length > 1) {
					cell.setCellValue(names[1]);
				} else {
					cell.setCellValue(colsStr[titleCol]);
				}
				cell.setCellStyle(cellStyle);
			}

			// �����ļ�����
			int pageSize = 20;
			int pageCount = count % pageSize == 0 ? (count / pageSize)
					: (count / pageSize) + 1;
			for (int i = 0; i <= pageCount; i++) {
				List list = null;
				// ��sql:��ʼ��˵����ִ��sql��䣬����ִ��hql���
				if (query.startsWith("sql:")) {
					String sql = query.substring(4);
					HibernateSQLCall sqlCall = new HibernateSQLCall(sql, i,
							pageSize);
					sqlCall.transformer = Transformers.ALIAS_TO_ENTITY_MAP;
					list = this.hibernateTemplate.executeFind(sqlCall);
				} else {
					list = this.hibernateTemplate
							.executeFind(new HibernateCall(query, i, pageSize));
				}
				for (int j = 0; j < list.size(); j++) {
					Object obj = list.get(j);
					// �ѵ���mapת����JSON����
					Map<String, Object> map = (Map<String, Object>) obj;
					rowNum++;
					row = sheet.createRow((short) rowNum);
					for (int z = 0; z < colsStr.length; z++) {
						// �õ�����
						String[] names = colsStr[z].split(":");
						// �б������ֶ���Ϊ��һ������������ֶ���
						String colName = colsStr[z];
						if (names.length > 1) {
							colName = names[0];
						}
						String data = "";
						if (map.get(colName) != null) {
							data = map.get(colName).toString();
						}
						cell = row.createCell((short) (z));
						// ����������
						cell.setCellType(HSSFCell.CELL_TYPE_STRING);
						// �����е��ַ���Ϊ����
						cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
						cell.setCellValue(data);
					}
				}
			}
			// ������ʱ�ļ�
			File file = File.createTempFile("temp", ".xls");
			file.deleteOnExit();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			// д�ļ�
			workbook.write(fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			// �����ļ���
			String result = file.getAbsolutePath();
			log.debug(result);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// ִ�з�ҳ��ѯ
	class HibernateCall implements HibernateCallback {
		String hql;
		int page;
		int rows;

		public HibernateCall(String hql, int page, int rows) {
			this.hql = hql;
			this.page = page;
			this.rows = rows;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createQuery(hql);
			List result = q.setFirstResult(page * rows).setMaxResults(rows)
					.list();
			return result;
		}
	}

	// ִ��sql��ҳ��ѯ���������ʽ��������
	class HibernateSQLCall implements HibernateCallback {
		String sql;
		int page;
		int rows;
		// ��ѯ���ת����������ת����Map�ȡ�
		public ResultTransformer transformer = null;

		public HibernateSQLCall(String sql, int page, int rows) {
			this.sql = sql;
			this.page = page;
			this.rows = rows;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createSQLQuery(sql);
			// ��ת����������ת����
			if (transformer != null) {
				q.setResultTransformer(transformer);
			}
			List result = q.setFirstResult(page * rows).setMaxResults(rows)
					.list();
			return result;
		}
	}
}