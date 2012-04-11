package thredds.server.ncSubset.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.LastModified;

import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DataRootHandler;
import ucar.nc2.dt.GridDataset;

public class AbstractNcssController implements LastModified{
	
	static private final Logger log = LoggerFactory.getLogger(AbstractNcssController.class);
	
	protected static final String NETCDF_FORMAT_NAME = "NETCDF";
	
	protected static final String servletPath = "/ncss/grid";
	
	protected static final String servletCachePath = "/cache/ncss";
	
	//The interceptor sets these properties 
	protected String requestPathInfo = null;
	protected GridDataset gridDataset = null;		
	
	void setRequestPathInfo(String requestPathInfo) {
		this.requestPathInfo = requestPathInfo;
	}

	public String getRequestPathInfo() {
		return this.requestPathInfo;
	}

	void setGridDataset(GridDataset gds) {
		this.gridDataset = gds;
	}

	public GridDataset getGridDataset() {
		return this.gridDataset;
	}
	
	
	protected void handleValidationErrorsResponse(HttpServletResponse response, int status, BindingResult  validationResult){
		
		List<ObjectError> errors = validationResult.getAllErrors();
		//String responseStr="Validation errors: ";
		StringBuffer responseStr = new StringBuffer();
		responseStr.append("Validation errors: ");
		for(ObjectError err : errors){			
			responseStr.append(err.getDefaultMessage());
			responseStr.append("  -- ");
		}
				
		try{
			
			PrintWriter pw = response.getWriter();
			pw.write(responseStr.toString() );
			pw.flush();
			
		}catch(IOException ioe){
			log.error(ioe.getMessage()); 
		}	
		
	}	
	
	protected void setResponseHeaders(HttpServletResponse response, HttpHeaders httpHeaders){
		
		Set<String> keySet = httpHeaders.keySet();
		Iterator<String> it = keySet.iterator();
		while( it.hasNext() ){
			String key = it.next();
			if(httpHeaders.containsKey(key)){
				response.setHeader(key, httpHeaders.get(key).get(0)  );
			}
			
		}	
	}
	
	protected SupportedFormat getSupportedFormat(RequestParamsBean params, SupportedOperation operation) throws UnsupportedResponseFormatException{
		
		//Cheking request format...
		SupportedFormat sf;		
		if(params.getAccept() == null){
			//setting the default format
			sf = SupportedFormat.values()[0];
			params.setAccept(sf.getAliases().get(0));
		}else{		
			sf = SupportedFormat.isSupportedFormat(params.getAccept(), operation);		
			if( sf == null ){			
				throw new UnsupportedResponseFormatException("Requested format: "+params.getAccept()+" is not supported for "+operation.getOperation().toLowerCase() );
			}
		}
		
		return sf;
		
	}
	
	
	public static final String getServletPath() {
		return AbstractNcssController.servletPath;
	}
	
	public static final String getServletCachePath() {
		return AbstractNcssController.servletCachePath;
	}	
	
	public static final String buildCacheUrl(String fileName){
		 return NcssRequestUtils.getTdsContext().getContextPath() + AbstractNcssController.getServletCachePath() + "/" + fileName;
	}
	
	public long getLastModified(HttpServletRequest req) {
		File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
		if ((file != null) && file.exists())
			return file.lastModified();
		return -1;
	}	
	
}
