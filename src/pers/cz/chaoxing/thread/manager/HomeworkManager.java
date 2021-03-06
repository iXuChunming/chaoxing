package pers.cz.chaoxing.thread.manager;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.impl.HomeworkCheckCodeCallBack;
import pers.cz.chaoxing.common.quiz.HomeworkQuizInfo;
import pers.cz.chaoxing.common.task.data.homework.HomeworkData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.thread.task.HomeworkTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class HomeworkManager implements Runnable {
    private Semaphore semaphore;
    private int homeworkThreadPoolCount;
    private ExecutorService homeworkThreadPool;
    private CompletionService<Boolean> homeworkCompletionService;
    private int homeworkThreadCount = 0;
    private List<Map<String, String>> paramsList;
    private String baseUri;
    private String cardUriModel;
    private boolean hasSleep;
    private boolean autoComplete;
    private CallBack<?> customCallBack;
    private CallBack<CallBackData> homeworkCallBack;

    public HomeworkManager(int homeworkThreadPoolCount) {
        this.homeworkThreadPoolCount = homeworkThreadPoolCount;
        if (this.homeworkThreadPoolCount > 0) {
            this.homeworkThreadPool = new ThreadPoolExecutor(homeworkThreadPoolCount, homeworkThreadPoolCount, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            this.homeworkCompletionService = new ExecutorCompletionService<>(homeworkThreadPool);
        }
        this.homeworkCallBack = new HomeworkCheckCodeCallBack("./checkCode-homework.jpeg");
    }

    @Override
    public void run() {
        if (this.homeworkThreadPoolCount > 0)
            try {
                for (Map<String, String> params : paramsList) {
                    acquire();
                    TaskInfo<HomeworkData> homeworkInfo;
                    while (true)
                        try {
                            homeworkInfo = CXUtil.getTaskInfo(baseUri, cardUriModel, params, InfoType.Homework);
                            break;
                        } catch (CheckCodeException e) {
                            customCallBack.call(e.getSession(), e.getUri());
                        }
                    release();
                    for (HomeworkData attachment : homeworkInfo.getAttachments()) {
                        while (true)
                            try {
                                HomeworkQuizInfo homeworkQuizInfo = CXUtil.getHomeworkQuizzes(baseUri, homeworkInfo, attachment);
                                if (!homeworkQuizInfo.isPassed()) {
                                    String homeworkName = attachment.getProperty().getTitle();
                                    System.out.println("Homework did not pass:" + homeworkName);
                                    HomeworkTask homeworkTask = new HomeworkTask(homeworkInfo, attachment, homeworkQuizInfo, baseUri);
                                    homeworkTask.setCheckCodeCallBack(homeworkCallBack);
                                    homeworkTask.setHasSleep(hasSleep);
                                    homeworkTask.setSemaphore(semaphore);
                                    homeworkTask.setAutoComplete(autoComplete);
                                    homeworkCompletionService.submit(homeworkTask);
                                    homeworkThreadCount++;
                                    System.out.println("Added homeworkTask to ThreadPool:" + homeworkName);
                                } else
                                    release();
                                break;
                            } catch (CheckCodeException e) {
                                customCallBack.call(e.getSession(), e.getUri());
                            }
                    }
                }
            } catch (RequestsException e) {
                System.out.println("Net connection error");
                release();
            } catch (Exception ignored) {
                release();
            }
        System.out.println("All homework task has been called");
    }

    private void acquire() throws InterruptedException {
        if (null != semaphore)
            semaphore.acquire();
    }

    private void release() {
        if (null != semaphore)
            semaphore.release();
    }

    public void setParamsList(List<Map<String, String>> paramsList) {
        this.paramsList = paramsList;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setCardUriModel(String cardUriModel) {
        this.cardUriModel = cardUriModel;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public void setCustomCallBack(CallBack<?> customCallBack) {
        this.customCallBack = customCallBack;
    }

    public void close() {
        try {
            for (int i = 0; i < homeworkThreadCount; i++)
                homeworkCompletionService.take().get();
        } catch (Exception ignored) {
        }
        if (this.homeworkThreadPoolCount > 0)
            homeworkThreadPool.shutdown();
        System.out.println("Finished homeworkTask count:" + homeworkThreadCount);
    }
}
