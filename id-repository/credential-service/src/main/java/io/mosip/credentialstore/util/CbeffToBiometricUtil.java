package io.mosip.credentialstore.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.bioapi.model.KeyValuePair;
import io.mosip.kernel.core.bioapi.spi.IBioApi;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIRInfo;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.RegistryIDType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;


/**
 * The Class CbeffToBiometricUtil.
 * 
 * @author M1048358 Alok
 * @author M1030448 Jyoti
 */
public class CbeffToBiometricUtil {
	

	/** The cbeffutil. */
	private CbeffUtil cbeffutil=new CbeffImpl();


	/**
	 * Instantiates a new cbeff to biometric util.
	 *
	 * @param cbeffutil
	 *            the cbeffutil
	 */
	public CbeffToBiometricUtil(CbeffUtil cbeffutil) {
		this.cbeffutil = cbeffutil;
	}
	
	/**
	 * Instantiates  biometric util
	 * 
	 */
	public CbeffToBiometricUtil() {
		
	}

	/**
	 * Gets the photo.
	 *
	 * @param cbeffFileString
	 *            the cbeff file string
	 * @param type
	 *            the type
	 * @param subType
	 *            the sub type
	 * @return the photo
	 * @throws Exception
	 *             the exception
	 */
	public byte[] getImageBytes(String cbeffFileString, String type, List<String> subType) throws Exception {


		byte[] photoBytes = null;
		if (cbeffFileString != null) {
			List<BIRType> bIRTypeList = getBIRTypeList(cbeffFileString);
			photoBytes = getPhotoByTypeAndSubType(bIRTypeList, type, subType);
		}


		return photoBytes;
	}

	/**
	 * Gets the photo by type and sub type.
	 *
	 * @param bIRTypeList
	 *            the b IR type list
	 * @param type
	 *            the type
	 * @param subType
	 *            the sub type
	 * @return the photo by type and sub type
	 */
	private byte[] getPhotoByTypeAndSubType(List<BIRType> bIRTypeList, String type, List<String> subType) {
		byte[] photoBytes = null;
		for (BIRType birType : bIRTypeList) {
			if (birType.getBDBInfo() != null) {
				List<SingleType> singleTypeList = birType.getBDBInfo().getType();
				List<String> subTypeList = birType.getBDBInfo().getSubtype();

				boolean isType = isSingleType(type, singleTypeList);
				boolean isSubType = isSubType(subType, subTypeList);

				if (isType && isSubType) {
					photoBytes = birType.getBDB();
					break;
				}
			}
		}
		return photoBytes;
	}

	/**
	 * Checks if is sub type.
	 *
	 * @param subType
	 *            the sub type
	 * @param subTypeList
	 *            the sub type list
	 * @return true, if is sub type
	 */
	private boolean isSubType(List<String> subType, List<String> subTypeList) {
		return subTypeList.equals(subType) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Checks if is single type.
	 *
	 * @param type
	 *            the type
	 * @param singleTypeList
	 *            the single type list
	 * @return true, if is single type
	 */
	private boolean isSingleType(String type, List<SingleType> singleTypeList) {
		boolean isType = false;
		for (SingleType singletype : singleTypeList) {
			if (singletype.value().equalsIgnoreCase(type)) {
				isType = true;
			}
		}
		return isType;
	}


	

	/**
	 * Convert BIRTYP eto BIR.
	 *
	 * @param listOfBIR
	 *            the list of BIR
	 * @return the list
	 */
	public List<BIR> convertBIRTYPEtoBIR(List<BIRType> listOfBIR) {
		
		return cbeffutil.convertBIRTypeToBIR(listOfBIR);
	}

	/**
	 * Gets the BIR type list.
	 *
	 * @param cbeffFileString
	 *            the cbeff file string
	 * @return the BIR type list
	 * @throws Exception
	 *             the exception
	 */
	public List<BIRType> getBIRTypeList(String cbeffFileString) throws Exception {
		return cbeffutil.getBIRDataFromXML(CryptoUtil.decodeBase64(cbeffFileString));
	}
	/**
	 * Gets the BIR type list.
	 * @param xmlBytes byte array of XML data
	 * @return the BIR type list
	 * @throws Exception
	 *             the exception
	 */
	public List<BIRType> getBIRDataFromXML(byte[] xmlBytes) throws Exception {
		return cbeffutil.getBIRDataFromXML(xmlBytes);
	}


}
