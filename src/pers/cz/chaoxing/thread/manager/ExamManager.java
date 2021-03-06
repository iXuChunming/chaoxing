package pers.cz.chaoxing.thread.manager;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.impl.ExamCheckCodeCallBack;
import pers.cz.chaoxing.common.quiz.ExamQuizInfo;
import pers.cz.chaoxing.common.task.data.exam.ExamData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.thread.task.ExamTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class ExamManager implements Runnable {
    private Semaphore semaphore;
    private int examThreadPoolCount;
    private ExecutorService examThreadPool;
    private CompletionService<Boolean> examCompletionService;
    private int examThreadCount = 0;
    private List<Map<String, String>> paramsList;
    private String baseUri;
    private String examUriModel;
    private boolean hasSleep;
    private boolean autoComplete;
    private CallBack<?> customCallBack;
    private CallBack<CallBackData> examCallBack;

    public ExamManager(int examThreadPoolCount) {
        this.examThreadPoolCount = examThreadPoolCount;
        if (this.examThreadPoolCount > 0) {
            this.examThreadPool = new ThreadPoolExecutor(examThreadPoolCount, examThreadPoolCount, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            this.examCompletionService = new ExecutorCompletionService<>(examThreadPool);
        }
        this.examCallBack = new ExamCheckCodeCallBack("./checkCode-exam.jpeg");
    }

    @Override
    public void run() {
        if (this.examThreadPoolCount > 0)
            try {
                for (Map<String, String> params : paramsList) {
                    TaskInfo<ExamData> examInfo;
                    while (true)
                        try {
                            examInfo = CXUtil.getTaskInfo(baseUri, examUriModel, params, InfoType.Exam);
                            break;
                        } catch (CheckCodeException e) {
                            customCallBack.call(e.getSession(), e.getUri());
                        }
                    for (ExamData attachment : examInfo.getAttachments()) {
                        if (!attachment.isPassed()) {
                            while (true)
                                try {
                                    boolean isAllowed;
                                    try {
                                        isAllowed = CXUtil.startExam(baseUri, examInfo, attachment);
                                    } catch (CheckCodeException e) {
                                        attachment.setEnc(examCallBack.call(e.getSession(), e.getUri(), attachment.getProperty().gettId(), examInfo.getDefaults().getClazzId(), examInfo.getDefaults().getCourseid(), "callback").getEnc());
                                        isAllowed = true;
                                    }
                                    if (isAllowed) {
                                        ExamQuizInfo examQuizInfo = CXUtil.getExamQuizzes(baseUri, examInfo, attachment);
                                        String examName = attachment.getProperty().getTitle();
                                        System.out.println("exam did not pass:" + examName);
                                        ExamTask examTask = new ExamTask(examInfo, attachment, examQuizInfo, baseUri);
                                        examTask.setCheckCodeCallBack(examCallBack);
                                        examTask.setHasSleep(hasSleep);
                                        examTask.setSemaphore(semaphore);
                                        examTask.setAutoComplete(autoComplete);
                                        examCompletionService.submit(examTask);
                                        examThreadCount++;
                                        System.out.println("Added examTask to ThreadPool:" + examName);
                                    }
                                    break;
                                } catch (CheckCodeException e) {
                                    customCallBack.call(e.getSession(), e.getUri());
                                }
                        }
                    }
                }
            } catch (RequestsException e) {
                System.out.println("Net connection error");
            } catch (Exception ignored) {
            }
        System.out.println("All exam task has been called");
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

    public void setExamUriModel(String examUriModel) {
        this.examUriModel = examUriModel;
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
            for (int i = 0; i < examThreadCount; i++)
                examCompletionService.take().get();
        } catch (Exception ignored) {
        }
        if (this.examThreadPoolCount > 0)
            examThreadPool.shutdown();
        System.out.println("Finished examTask count:" + examThreadCount);
    }
}
