package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.Tag;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class GitManager {
  private final Logger log = LoggerFactory.getLogger(GitManager.class);
  private Git git;

  public GitManager() throws IOException, GitAPIException {
    log.info("start initRepository()");
    String usrHome = System.getProperty("user.home");
    File repositoryDir = Paths.get(usrHome, fun.hydd.cddabrowser.Constants.REPOSITORY_CDDA).toFile();
    if (repositoryDir.exists()) {
      log.info("repositoryDir '{}' exist", repositoryDir);
      FileRepositoryBuilder fileRepositoryBuilder = new FileRepositoryBuilder();
      File gitDir = Paths.get(repositoryDir.getAbsolutePath(), ".git").toFile();
      try (
        Repository repository = fileRepositoryBuilder
          .setGitDir(gitDir)
          .readEnvironment()
          .findGitDir()
          .build()
      ) {
        log.info("find repository: {}", repository.getDirectory());
        this.git = new Git(repository);
      }
    } else {
      if (repositoryDir.mkdir()) {
        log.info("repositoryDir '{}' mkdir", repositoryDir);
        try (
          Git newGit = Git.cloneRepository()
            .setDirectory(repositoryDir)
            .setURI(fun.hydd.cddabrowser.Constants.URL_REPOSITORY_GIT_CDDA)
            .setBranch("master")
            .call()
        ) {
          Repository repository = newGit.getRepository();
          log.info("clone repository: {}", repository.getDirectory());
          this.git = new Git(repository);
        }
      } else {
        log.info("repositoryDir '{}' mkdir fail", repositoryDir);
      }
    }
  }

  public void update() throws GitAPIException {
    log.info("start update");
    FetchResult fetchResult = git.pull().setRemote(Constants.DEFAULT_REMOTE_NAME).setRemoteBranchName(Constants.MASTER).call().getFetchResult();
    log.info("update result");
    for (TrackingRefUpdate trackingRefUpdate : fetchResult.getTrackingRefUpdates()) {
      log.info("\t{}", trackingRefUpdate);
    }
  }

  public void reset(String tagName) throws GitAPIException {
    git.reset()
      .setMode(ResetCommand.ResetType.HARD)
      .setRef(tagName)
      .call();
  }

  public List<Ref> getLocalNoHasRemoteTagRefList() throws GitAPIException {
    log.info("start getLocalNoHasRemoteTagRefList()");
    Collection<Ref> remoteRefs = git.lsRemote()
      .setRemote(fun.hydd.cddabrowser.Constants.URL_REPOSITORY_GIT_CDDA)
      .setTags(true)
      .call();
    log.info("remoteRefs size is {}", remoteRefs.size());
    List<Ref> localRefs = git.tagList()
      .call();
    log.info("localRefs size is {}", localRefs.size());
    remoteRefs = remoteRefs
      .stream()
      .filter(
        ref -> localRefs
          .stream()
          .noneMatch(
            ref1 -> ref
              .getName()
              .equals(ref1.getName())
          )
      )
      .collect(Collectors.toList());
    return List.copyOf(remoteRefs);
  }

  public Ref getHeadRef() throws IOException {
    return git.getRepository().getRefDatabase().findRef(Constants.R_HEADS + Constants.MASTER);
  }

  public Tag getHeadTag() throws IOException {
    log.info("start getHeadTag()");
    Ref headRef = getHeadRef();
    log.info("\theadRef is {}", headRef);
    ObjectId hashObjectId = headRef.getObjectId();
    Ref headTagRef = getTagRef(hashObjectId);
    if (headTagRef == null) {
      log.info("\thead no has tag, headRef is {}", headRef);
      return null;
    }
    log.info("\theadTagRef is {}", headTagRef);
    return getTagByTagRef(headTagRef);
  }

  public Tag getLatestTag() throws IOException {
    log.info("start getLatestTag()");
    Ref latestTagRef = getLatestTagRef();
    if (latestTagRef == null) {
      log.warn("\tgetLatestTag() no find tag");
      return null;
    }
    log.info("\tlatestTagRef is {}", latestTagRef);
    return getTagByTagRef(latestTagRef);
  }

  public Ref getLatestTagRef() {
    log.info("start getLatestTagRef()");
    Ref result = null;
    Date latestDate = null;
    try {
      List<Ref> tagRefs = git.tagList().call();
      for (Ref tagRef : tagRefs) {
        Date currentDate = getTagRefDate(tagRef);
        if (latestDate == null || (currentDate != null && currentDate.after(latestDate))) {
          result = tagRef;
          latestDate = currentDate;
        }
      }
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
    return result;
  }

  public Tag getTagByTagRef(Ref tagRef) throws IOException {
    Tag tag = new Tag();
    try (RevWalk revWalk = new RevWalk(git.getRepository())) {
      RevObject revObject = revWalk.parseAny(tagRef.getObjectId());
      if (Constants.OBJ_TAG == revObject.getType()) {
        RevTag revTag = (RevTag) revObject;
        tag.setName(revTag.getTagName());
        tag.setDate(revTag.getTaggerIdent().getWhen());
        tag.setMessage(revTag.getFullMessage());
      } else if (Constants.OBJ_COMMIT == revObject.getType()) {
        RevCommit revCommit = (RevCommit) revObject;
        tag.setName(Repository.shortenRefName(tagRef.getName()));
        tag.setDate(revCommit.getAuthorIdent().getWhen());
      }
    }
    return tag;
  }

  private Date getTagRefDate(Ref tagRef) {
    if (tagRef == null) {
      log.warn("getTagRefDate() parameter has null");
      return null;
    }
    Date result = null;
    try (RevWalk revWalk = new RevWalk(git.getRepository())) {
      try {
        RevObject revObject = revWalk.parseAny(tagRef.getObjectId());
        if (Constants.OBJ_TAG == revObject.getType()) {
          RevTag revTag = (RevTag) revObject;
          result = revTag.getTaggerIdent().getWhen();
        } else if (Constants.OBJ_COMMIT == revObject.getType()) {
          RevCommit revCommit = (RevCommit) revObject;
          result = revCommit.getAuthorIdent().getWhen();
        } else {
          log.warn("getTagRefDate() ragRef no tag or commit, type is {}, id is {}", revObject.getType(), tagRef.getObjectId());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  private Ref getTagRef(ObjectId commitObjectId) {
    if (commitObjectId == null) {
      log.warn("getTagRef() parameter has null");
      return null;
    }
    List<Ref> result = new ArrayList<>(1);
    try (RevWalk revWalk = new RevWalk(git.getRepository())) {
      try {
        List<Ref> tagRefs = git.tagList().call();
        for (Ref tagRef : tagRefs) {
          if (equalTagRefAndCommitObjectId(tagRef, commitObjectId, revWalk)) {
            result.add(tagRef);
          }
        }
      } catch (GitAPIException | IOException e) {
        e.printStackTrace();
      }
    }
    if (result.size() == 1) {
      return result.get(0);
    } else if (result.isEmpty()) {
      log.warn("getTagRef result size is 0, commitObjectId is {}", commitObjectId);
      return null;
    } else {
      log.warn("getTagRef result size is greater 1, commitObjectId is {}", commitObjectId);
      for (Ref wrongResult : result) {
        log.warn("\tname is {}, objectId is {}.", wrongResult.getName(), wrongResult.getObjectId());
      }
      return result.get(0);
    }
  }

  private boolean equalTagRefAndCommitObjectId(Ref tagRef, ObjectId commitObjectId, RevWalk revWalk) throws IOException {
    if (tagRef == null || commitObjectId == null || revWalk == null) {
      log.warn("equalTagRefAndCommitObjectId() parameter has null, tagRef: {}, commitObjectId: {}, revWalk: {}.", tagRef, commitObjectId, revWalk);
      return false;
    }
    RevObject revObject = revWalk.parseAny(tagRef.getObjectId());
    if (Constants.OBJ_TAG == revObject.getType()) {
      RevTag revTag = (RevTag) revObject;
      RevObject peeled = revWalk.peel(revTag.getObject());
      return commitObjectId.equals(peeled.getId());
    } else if (Constants.OBJ_COMMIT == revObject.getType()) {
      return commitObjectId.equals(tagRef.getObjectId());
    } else {
      log.warn("equalTagRefAndCommitObjectId() ragRef no tag or commit, type is {}, commitObjectId is {}", revObject.getType(), commitObjectId);
    }
    return false;
  }
}
