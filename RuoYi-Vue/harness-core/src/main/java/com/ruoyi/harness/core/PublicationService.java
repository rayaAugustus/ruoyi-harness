package com.ruoyi.harness.core;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.core.domain.HarnessApp;
import com.ruoyi.harness.core.domain.HarnessAppVersion;
import com.ruoyi.harness.core.port.HarnessAppRepository;
import com.ruoyi.harness.core.port.HarnessVersionRepository;
import java.time.Instant;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

public class PublicationService {
    private static final Set<VersionStatus> ROLLBACK_TARGETS=Set.of(VersionStatus.PUBLISHED,VersionStatus.SUPERSEDED);
    private final AppRegistryService appService; private final VersionService versionService;
    private final HarnessAppRepository apps; private final HarnessVersionRepository versions; private final PublishedArtifactCache cache;
    public PublicationService(AppRegistryService a,VersionService v,HarnessAppRepository ar,HarnessVersionRepository vr,PublishedArtifactCache c){appService=a;versionService=v;apps=ar;versions=vr;cache=c;}
    @Transactional public void publish(String appKey,Long versionId,Long actorId){switchPointer(appKey,versionId,actorId,false);}
    @Transactional public void rollback(String appKey,Long versionId,Long actorId){switchPointer(appKey,versionId,actorId,true);}
    private void switchPointer(String appKey,Long versionId,Long actorId,boolean rollback){
        // The row lock serializes all pointer transitions for one application. The database remains authoritative.
        HarnessApp app=appService.requireLockedEntity(appKey); HarnessAppVersion target=versionService.requireEntity(app,versionId);
        if ((!rollback && target.getStatus()!=VersionStatus.VALIDATED)||(rollback&&!ROLLBACK_TARGETS.contains(target.getStatus())))
            throw new HarnessException(HarnessErrorCode.VERSION_STATE_INVALID,rollback?"Rollback target is not immutable and valid":"Only a validated version can be published");
        if (app.getPublishedVersionId()!=null&&!app.getPublishedVersionId().equals(versionId)){
            HarnessAppVersion current=versions.findById(app.getPublishedVersionId()); if(current!=null)versions.updateStatus(current.getId(),VersionStatus.SUPERSEDED,current.getPublishedAt());
        }
        Instant now=Instant.now(); versions.updateStatus(target.getId(),VersionStatus.PUBLISHED,target.getPublishedAt()==null?now:target.getPublishedAt());
        apps.setPublishedVersion(app.getId(),target.getId(),actorId); cache.invalidateApp(appKey);
    }
}
