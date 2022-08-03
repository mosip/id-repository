package io.mosip.credentialstore.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class VIDUtil {

    @Autowired
    RestUtil restUtil;

    @Autowired
    private ObjectMapper mapper;

    private static final Logger LOGGER = IdRepoLogger.getLogger(VIDUtil.class);

    @Value("${mosip.idrepo.vid.id.create:mosip.vid.create}")
    private  String applicationId;

    @Value("${mosip.idrepo.vid.application.version:v1}")
    private  String version;

    private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

    @Autowired
    private Environment env;

    public VidInfoDTO getVIDData(String uin,String vidType,String vid) throws ApiNotAccessibleException, IdRepoException {
        List<String> pathVariables=new ArrayList<>();
        pathVariables.add(uin);
        ResponseWrapper<List<VidInfoDTO>> response=null;
        List<VidInfoDTO> vidResponseDTO=new ArrayList<>();
        List<VidInfoDTO> vidInfoDTOS=new ArrayList<>();

        try {
            response=restUtil.getApi(ApiName.RETRIEVE_VID,pathVariables,"","",ResponseWrapper.class);
            if(response.getResponse()!=null && !response.getResponse().isEmpty()) {
                vidResponseDTO = mapper.readValue(mapper.writeValueAsString(response.getResponse()), List.class);
                for (Object infoDTO : vidResponseDTO) {
                    VidInfoDTO vidInfoDTO = mapper.readValue(mapper.writeValueAsString(infoDTO), VidInfoDTO.class);
                    vidInfoDTOS.add(vidInfoDTO);
                }
                if(vid!=null) {
                    vidInfoDTOS = vidInfoDTOS.stream().filter(vidInfoDTO -> vidInfoDTO.getVidType().equalsIgnoreCase(vidType) && vidInfoDTO.getVid().equals(vid)).collect(Collectors.toList());
                    return vidInfoDTOS.get(0);
                }
                vidInfoDTOS = vidInfoDTOS.stream().filter(vidInfoDTO -> vidInfoDTO.getVidType().equalsIgnoreCase(vidType)).collect(Collectors.toList());
                if(!vidInfoDTOS.isEmpty()) {
                    vidInfoDTOS.sort(Comparator.comparing(VidInfoDTO::getExpiryTimestamp).reversed());
                    return vidInfoDTOS.get(0);
                }
            }
        } catch (Exception e) {
            LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), uin,
                    ExceptionUtils.getStackTrace(e));
            if (e.getCause() instanceof HttpClientErrorException) {
                HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
                throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(
                        httpClientException.getResponseBodyAsString());
            } else if (e.getCause() instanceof HttpServerErrorException) {
                HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
                throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
            } else {
                throw new IdRepoException(e);
            }
        }
        return null;
    }

    public VidResponseDTO generateVID(String uin, String vidType) throws ApiNotAccessibleException, IdRepoException {
        VidRequestDTO vidRequestDTO=new VidRequestDTO();
        vidRequestDTO.setUin(uin);
        vidRequestDTO.setVidType(vidType);
        RequestWrapper<VidRequestDTO> vidRequestDTORequestWrapper=new RequestWrapper<>();
        ResponseWrapper<VidResponseDTO> vidResponseDTOResponseWrapper=null;
        vidRequestDTORequestWrapper.setRequest(vidRequestDTO);
        String response=null;
        VidResponseDTO vidResponseDTO;
        try {
            vidRequestDTORequestWrapper.setId(applicationId);
            vidRequestDTORequestWrapper.setVersion(version);
            DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
            LocalDateTime localdatetime = LocalDateTime
                    .parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
            vidRequestDTORequestWrapper.setRequesttime(localdatetime);
            vidResponseDTOResponseWrapper=restUtil.postApi(ApiName.GENERATE_VID,null,"","", MediaType.APPLICATION_JSON,vidRequestDTORequestWrapper,ResponseWrapper.class);
            vidResponseDTO = mapper.readValue(mapper.writeValueAsString(vidResponseDTOResponseWrapper.getResponse()), VidResponseDTO.class);
        } catch (Exception e) {
            LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), uin,
                    ExceptionUtils.getStackTrace(e));
            if (e.getCause() instanceof HttpClientErrorException) {
                HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
                throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(
                        httpClientException.getResponseBodyAsString());
            } else if (e.getCause() instanceof HttpServerErrorException) {
                HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
                throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
            } else {
                throw new IdRepoException(e);
            }
        }
        return vidResponseDTO;
    }

}
