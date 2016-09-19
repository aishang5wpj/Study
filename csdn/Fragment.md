##前言
今天是加入小红书的第一个月，工作已经慢慢步入正轨了，除了工作时间稍微有点长以外还是挺喜欢这里的，不过作为即使下了班也没人约的单身狗，想想也觉得无所谓了。

这算是自我安慰吗，哈哈哈~

老大说以后Android Team一周内要分享两次知识，让我们自己下去准备，每个人都要分享自己的知识。我觉得这点蛮好的，每个人做开发都有自己的经验和技巧，虽然现在很多东西网上都有，不过如果能面对面的分享出来，互相交流学习，这样更容易成长。

本来一开始也是准备讲Fragment相关的一些坑的，自己也收集了一些，比如Fragment嵌套、Fragment重叠、状态保持balabala，后来网上已经有大神总结的非常全面、细致了，而且有些东西自己也没有他理解的透彻，所以就不想再照本宣科的把人家总结的东西再重复糊弄一遍。

这是那几篇文章，有需要的朋友可以看看：

- [Fragment全解析（1）：那些年踩过的坑](http://android.jobbole.com/83072/)
- [Fragment全解析（2）：正确的使用姿势](http://android.jobbole.com/83073/)
- [Fragment全解析（3）：我的解决方案](http://android.jobbole.com/83074/)

在找资料的过程中，脑海里其实也一直在想一个问题，用了这么久的Fragment，各种add、hide、show，有没有想过它到底是怎么add、hide、show的呢，然后就去翻了下源码，发现关于Fragment想过的源码其实并不是很复杂，所以干脆就把主要的流程都看了一遍，于是也就有了这篇文章。

那我们开始吧！

注：本人阅读是support-v4-24.1.1中Fragment相关的源码，其他版本可能略有出入，请注意

##从哪里开始呢
阅读源码需要一个切入点，回想一下平时使用Fragment时我们写的代码，如下：

```
FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
ft.setCustomAnimations(0,0,0,0)
  .add(R.id.container, new ShowFragment(), "ShowFragment")
  .hide(hideFragment)
  .addToBackStack("ShowFragment")
  .commit();
```

上面代码的第一行，可以看到getSupportFragmentManager()即是我们使用Fragment最先开始的地方，那我们就从这里开始。

点进去，源码如下：

```
    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this activity.
     */
    public FragmentManager getSupportFragmentManager() {
        return mFragments.getSupportFragmentManager();
    }
```

getSupportFragmentManager()是FragmentActivity中的方法，虽然现在一般继承的AppCompatActivity都自动继承了FragmentActivity，但是原来都要我们自己去继承FragmentActivity，直接继承Activity是没有的哦~

方法中的mFragments是FragmentActivity中的一个成员对象，是FragmentContorller类的实例。

那FragmentContorller又是什么呢？顾名思义，就是Activity控制Fragment的代理对象，封装了很多Fragment相关的操作，包括分发各种生命周期、保持状态等等。Activity不直接与FramgentManager打交道，都是通过FragmentContorller来完成的。

而FragmentContorller其实也只是做了一层封装，它的各种方法都是通过FragmentHostCallback的子类HostCallbacks的对象来代理实现的。

由于这块跟我们要讲的内容相对而言关系较弱，加上我上面几句话的逻辑其实并不复杂，为了不啰嗦所以就不贴代码了。

总之大家只需要各种具体的代码细节最后都是在FragmentManagerImpl中完成的即可，而getSupportFragmentManager()返回的FragmentManager只是一个接口而已，顾名思义，FragmentManagerImpl即是FragmentManager具体实现类，所以我们主要重点要看的还是FragmentManagerImpl中的一块内容。

说了这么久是不是看到不耐烦了，别着急，现在就进入主题吧！

##进入主题

还记得刚开始我们的切入点吗？一切的一切都是从下面这句话开始的：

```
FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
```
然后后续的各种add、show、hide操作都是通过上面的FragmentTransaction对象ft完成的。

刚才我们说getSupportFragmentManager()返回的FragmentManager其实是FragmentManagerImpl对象，那我们看看FragmentManagerImpl类中的beginTransaction()方法吧。

```
	@Override
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }
```
直接返回了一个BackStackRecord类的对象，来看看这个类的声明：

```
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, Runnable {
        }
```

可以看到它继承了 FragmentTransaction，而且还实现了FragmentManager.BackStackEntry、Runnable两个接口。

BackStackEntry是回退栈的接口，那为什么要继承Runnable接口呢？

因为在FragmentManagerImpl中有一个`ArrayList<Runnable> mPendingActions`对象，FragmentTransaction 对象的每一次commit()操作并不是立即就执行，它们其实都只是相当于一个BackStackEntry被强转成Runnable添加到上面的mPendingActions中，所以当FragmentManagerImpl需要真正开始执行你的这些commit指令时，它会自动遍历上面的mPendingActions集合，并依次调用它们的run方法，然后这些操作才能被真正执行。

而FragmentManagerImpl并不需要知道这些操作的全部细节，而且Fragment的各种相关操作实际上最后还是在BackStackEntry中完成的，所以当FragmentManagerImpl要执行某次操作时，FragmentManagerImpl只需要调用BackStackEntry的run方法告诉它“我要开始执行你的xx操作了”，而“xx操作”具体该怎么做就由BackStackEntry来决定。

所以这其中有一个责任转移的过程，FragmentManagerImpl和BackStackEntry职责分明、各司其职。

扯远了，上面的细节可以在后面验证，回到原来的分析吧。

总结一下目前为止分析到的结果：

- FragmentActivity中通过getFragmentManager()得到一个FragmentManagerImpl对象
- FragmentManagerImpl通过beginTransaction()方法返回一个实现了FragmentTransaction接口的BackStackEntry的实例
- 最后通过BackStackEntry执行Fragment的各种add、show、hide操作

先看看BackStackEntry的add()方法：

```
	public FragmentTransaction add(Fragment fragment, String tag) {
        doAddOp(0, fragment, tag, OP_ADD);
        return this;
    }

    public FragmentTransaction add(int containerViewId, Fragment fragment) {
        doAddOp(containerViewId, fragment, null, OP_ADD);
        return this;
    }

    public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
        doAddOp(containerViewId, fragment, tag, OP_ADD);
        return this;
    }
```
可以看到，最终都是调用的doAddOp()方法。doAddOp()有4个参数，前面3个好理解，需要注意的是最后一个参数，表示要执行的命令。一共有下面8种：

```
    static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;
```
一目了然，就不多讲了。来看看doAddOp()方法吧：

```
    private void doAddOp(int containerViewId, Fragment fragment, String tag, int opcmd) {
        fragment.mFragmentManager = mManager;

        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment "
                        + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }

        if (containerViewId != 0) {
            if (containerViewId == View.NO_ID) {
                throw new IllegalArgumentException("Can't add fragment "
                        + fragment + " with tag " + tag + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment "
                        + fragment + ": was " + fragment.mFragmentId
                        + " now " + containerViewId);
            }
            fragment.mContainerId = fragment.mFragmentId = containerViewId;
        }

        Op op = new Op();
        op.cmd = opcmd;
        op.fragment = fragment;
        addOp(op);
    }
```
代码的第4~11行，用来给Fragment设置Tag，第一次调用doAddOp()方法的时候，因为fragment.mTag==null，所以`if (fragment.mTag != null && !tag.equals(fragment.mTag))`肯定是不成立的，程序继续往下走，然后fragment.mTag被赋值成参数中的tag。

如果重复add同一个Fragment对象，而且两次add()的tag不一样，`if (fragment.mTag != null && !tag.equals(fragment.mTag))`就会成立，然后抛出上面的IllegalArgumentException异常。

所以对于同一个fragment对象，不要用不同的tag添加两次。以前有不明白的同学，现在是不是清楚啦~

当然我们上面讨论的都是基于add()时tag!=null时候的情况，如果你没给它设置tag，那你当我没说。

第13~24行，给fragment设置布局id，同样如果同一个fragment对象两次添加的布局不一样的话，也会报错。

第26~29行，如果说前面的代码都是为了设置fragment的布局、tag信息，那这里真正就是BackStackEntry执行的添加命令了。

这里有个Op类，代码如下：

```
    static final class Op {
        Op next; //上一条指令
        Op prev; //下一条指令
        int cmd; //指令类型，就是之前的那8种
        Fragment fragment; //指令的目标fragment
        int enterAnim; //进入动画
        int exitAnim;  //退出动画
        int popEnterAnim; //压栈动画
        int popExitAnim;  //出栈动画
        ArrayList<Fragment> removed; 
    }
```

这种结构是不是很眼熟？简直跟我们在学校学数据结构时的双向链表一毛一样啊~

没错，这就是一个双向链表的数据结构，支持从前往后、从后往前两种方式对链表进行遍历。在BackStackEntry中可以看到，还有两个Op对象mHead、mTail分别记录了链表的表头和表尾，同时有一个int参数mNumOp来记录一共有多少条命令。

Op的几个属性上面都写了注释，对于最后一个`ArrayList<Fragment> removed`要单独提一下，这个集合的作用是为了记录执行`OP_REPLACE `命令时被FragmentManager移除掉的所有fragment，为什么要记录呢？

大家都知道FragmentTransaction中的各种操作都是可以支持撤销的，所以如果我们用一个Fragment对象replace掉了若干个fragment，那当我们执行撤销的命令时，可以很方便的从上面的removed集合中找回那些曾经被replace掉的fragment，然后FragmentManager重新执行一遍add操作就可以还原成replace之前的样子啦~

回到刚才的分析：FragmentTransaction的add()方法其实是通过BackStackEntry的doAddOp()来完成的，而doAddOp()方法中把这个add操作封装成Op对象之后，又调用addOp()方法来执行最终的添加命令。

下面是addOp()方法的代码：

```
    void addOp(Op op) {
        if (mHead == null) {
            mHead = mTail = op;
        } else {
            op.prev = mTail;
            mTail.next = op;
            mTail = op;
        }
        op.enterAnim = mEnterAnim;
        op.exitAnim = mExitAnim;
        op.popEnterAnim = mPopEnterAnim;
        op.popExitAnim = mPopExitAnim;
        mNumOp++;
    }
```
一目了然，很普通的插入链表的操作。

至此，我们代码中FragmentTransaction的add()操作的源码已经走读完了，包括其他的`OP_REPLACE `、`OP_HIDE`等等，这些过程走的逻辑都一样，唯一不同的只是doAddOp()方法中opcmd参数的值不同而已。

所以我们直接分析commit()操作，因为这里才是上面所有操作最后真正提交的命令。

```
    public int commit() {
        return commitInternal(false);
    }
```
commit()方法很简单，直接调用了commitInternal()方法：

```
    int commitInternal(boolean allowStateLoss) {
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", null, pw, null);
        }
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex(this);
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }
```
代码第1行，检查本次事务中所有命令是否已经提交过，如果没有则往下走。

第8行，标记本次事务的所有命令已提交，如果多次commit的话，第1行那句代码就会抛异常了。

第9行，mAddToBackStack是在哪里赋值的呢？顾名思义，直接搜索addToBackStack()方法，如下:

```
    public FragmentTransaction addToBackStack(String name) {
        if (!mAllowAddToBackStack) {
            throw new IllegalStateException(
                    "This FragmentTransaction is not allowed to be added to the back stack.");
        }
        mAddToBackStack = true;
        mName = name;
        return this;
    }
```
代码第1行，先检查该BackStackRecord是否可以被添加到后退栈，mAllowAddToBackStack是在哪里赋值的呢？搜索一下，找到disallowAddToBackStack()方法，代码如下：

```
    public FragmentTransaction disallowAddToBackStack() {
        if (mAddToBackStack) {
            throw new IllegalStateException(
                    "This transaction is already being added to the back stack");
        }
        mAllowAddToBackStack = false;
        return this;
    }
```
在FragmentTransaction中可以调用disallowAddToBackStack()方法来禁止当前BackStackRecord实例被添加到后退栈中，当然如果当前BackStackRecord已经被添加到后退栈中了，再次调用disallowAddToBackStack()就会抛出异常。

当我们在Activity中调用`FragmentTransaction.addToBackStack()`决定将一个Fragment添加到后退栈时，`mAddToBackStack`被赋值成`true`。所以`addToBackStack ()`和`disallowAddToBackStack()`这两个方法其实是相互关联和影响的。

回到commitInternal方法，当BackStackRecord添加到后退栈中之后，代码中的`if (mAddToBackStack)`就会成立，然后FragmentManagerImpl就会调用allocBackStackIndex()方法：

```
    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (mAvailBackStackIndices == null || mAvailBackStackIndices.size() <= 0) {
                if (mBackStackIndices == null) {
                    mBackStackIndices = new ArrayList<BackStackRecord>();
                }
                int index = mBackStackIndices.size();
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.add(bse);
                return index;

            } else {
                int index = mAvailBackStackIndices.remove(mAvailBackStackIndices.size()-1);
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.set(index, bse);
                return index;
            }
        }
    }
```
这个方法的作用就是将当前BackStackState实例添加到后退栈中，并且将当前记录保存后的索引作为方法的返回值返回。

回到commitInternal()方法中，代码第10行中就可以用mIndex保存本次记录在后退栈中的索引；如果没有调用addToBackStack()，mIndex则为-1。

第14行，将本次事务插入FragmentManager的事务队列。下面是enqueueAction()方法的代码。

```
    public void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (mDestroyed || mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (mPendingActions == null) {
                mPendingActions = new ArrayList<Runnable>();
            }
            mPendingActions.add(action);
            if (mPendingActions.size() == 1) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
            }
        }
    }
```
代码第1~3行，由于上面传进来的allowStateLoss是false，顾名思义，即不允许信息丢失，所以在checkStateLoss()方法中检查数据是否完整。

```
    private void checkStateLoss() {
        if (mStateSaved) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
        if (mNoTransactionsBecause != null) {
            throw new IllegalStateException(
                    "Can not perform this action inside of " + mNoTransactionsBecause);
        }
    }
```
具体哪些时候数据不完整呢？搜了一下mStateSaved=ture的时候的情况，有下面两种：

- 第一种是saveAllState()方法中，官方也给出了原因。
> 在Android3.0以后（基本上也就是当今的所有安卓设备了），我们在Fragment暂停后保存状态，3.0以前是在Fragment暂停之前。这将导致一个问题，因为有很多事情你可能在Fragment的暂停之后但在停止之前的这段时间完成，并且将改变Fragment的状态。
> 
> 对于那些较旧的设备，我们无法保证我们已经保存了所有的状态，所以我们将允许他们继续完成Fragment的所有事务。这将跟Android3.0一样，当进程被杀死时，你仍然有很大的风险丢失掉几乎全部的状态...这一点我们可以忍受。

- 第二种是在dispatchStop()方法中，原因跟上面一样。 

总之，在Fragment进入onStop()之后，不允许有任何的add、show、hide操作。

而对于上面的`if (mNoTransactionsBecause != null)`，暂时还不清楚这个条件会在什么情况下满足，有知道的朋友还请不吝赐教。

回到enqueueAction()方法，checkStateLoss()方法执行完之后：

代码第4行，先来一记同步锁。

第5~7行，检查Activity是否已经destroy。

第8~11行，将BackStackEntry强转之后的runnable添加到mPendingActions队列中。

第12~15行，限制了当Fragment事务队列中只有1条事务时，才执行真正的commit操作。

可为什么要加这个限制呢？首先，一开始mPendingActions肯定是null的，所以第一次commit的时候可以保证能够真正提交。可是有没有`mPendingActions.size() > 1`的时候呢？

由于这里有同步锁，加上这句代码`mHost.getHandler().post(mExecCommit)`，表明是用Activity中的Hander.post()的方式发起commit的命令（即切换到主线程），所以我们这里有理由怀疑`mPendingActions.size() > 1`时候的情形是多线程模式下多次commit的结果。

在多线程的情况下，多次commit时虽然无法执行`mHost.getHandler().post(mExecCommit)`操作，但是是不影响上一句代码`mPendingActions.add(action)`的执行的，也就是虽然无法继续发起提交的命令，但是mPendingActions中仍然是可以继续添加命令的。

而当mPendingActions中的任务都被执行完(即`mPendingActions.size() == 0`)时，虽然BackStackEntry中的run方法可能还没执行完，但是此时提交任务的话`mPendingActions.size() == 1`的条件又满足了，所以又可以重新发起一次提交任务的请求。

那如果没有`if (mPendingActions.size() == 1)`这个限制的话会怎么样呢？代码会频繁的`removeCallbacks(mExecCommit)、post(mExecCommit)`，这样有可能前面的n条任务没执行完就被移除，但是后面的任务又来了，这样无法保证多线程下执行结果的正确性。而且频繁的removeCallbacks、post操作对性能来说也是有一定影响的。

这样分析的话，是不是就容易说得通了。

当然我们平时开发中用多线程的方式来commit一次fragment的操作好像并不多见，所以这里也只是我的猜测，如果有朋友有相关经验的还请不吝赐教。

继续看源码，找到mExecCommit对象，代码如下：

```
    Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions();
        }
    };
```
直接调用了execPendingActions()方法，代码如下：

```
    public boolean execPendingActions() {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }
        
        if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }

        boolean didSomething = false;

        while (true) {
            int numActions;
            
            synchronized (this) {
                if (mPendingActions == null || mPendingActions.size() == 0) {
                    break;
                }
                
                numActions = mPendingActions.size();
                if (mTmpActions == null || mTmpActions.length < numActions) {
                    mTmpActions = new Runnable[numActions];
                }
                mPendingActions.toArray(mTmpActions);
                mPendingActions.clear();
                mHost.getHandler().removeCallbacks(mExecCommit);
            }
            
            mExecutingActions = true;
            for (int i=0; i<numActions; i++) {
                mTmpActions[i].run();
                mTmpActions[i] = null;
            }
            mExecutingActions = false;
            didSomething = true;
        }
        
        doPendingDeferredStart();

        return didSomething;
    }
```
代码第1行，检查当前是否已经在执行所有任务。

第5~7行，检查是否在主线程。

第9行，记录本次执行的结果是否成功，并作为execPendingActions()方法的返回值返回。

第11~26行，遍历mPendingActions中的所有任务，并依次执行所有的任务。
具体操作是先将mPendingActions转成Runnable数组mTmpActions，然后for循环遍历mTmpActions来执行每个runnable的run方法。

如果mTmpActions执行完毕，继续重新开始遍历mPendingActions，因为此时mPendingActions可能又被添加进了别的任务。

因此这里的while是死循环，除非`mPendingActions == null `或者`mPendingActions.size() == 0`。

还记得最开始我们讨论的为什么BackStackEntry要实现Runnable接口的原因吗？看到这里是不是豁然开朗了。

既然核心的过程都在BackStackEntry的run方法中完成了，那还是回到BackStackEntry中查看run方法吧。

```
    public void run() {
        if (FragmentManagerImpl.DEBUG) Log.v(TAG, "Run: " + this);

        if (mAddToBackStack) {
            if (mIndex < 0) {
                throw new IllegalStateException("addToBackStack() called after commit()");
            }
        }

        bumpBackStackNesting(1);

        TransitionState state = null;
        SparseArray<Fragment> firstOutFragments = null;
        SparseArray<Fragment> lastInFragments = null;
        if (SUPPORTS_TRANSITIONS && mManager.mCurState >= Fragment.CREATED) {
            firstOutFragments = new SparseArray<Fragment>();
            lastInFragments = new SparseArray<Fragment>();

            calculateFragments(firstOutFragments, lastInFragments);

            state = beginTransition(firstOutFragments, lastInFragments, false);
        }

        int transitionStyle = state != null ? 0 : mTransitionStyle;
        int transition = state != null ? 0 : mTransition;
        Op op = mHead;
        while (op != null) {
            int enterAnim = state != null ? 0 : op.enterAnim;
            int exitAnim = state != null ? 0 : op.exitAnim;
            switch (op.cmd) {
                case OP_ADD: {
                    Fragment f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.addFragment(f, false);
                } break;
                case OP_REPLACE: {
                    Fragment f = op.fragment;
                    int containerId = f.mContainerId;
                    if (mManager.mAdded != null) {
                        for (int i = mManager.mAdded.size() - 1; i >= 0; i--) {
                            Fragment old = mManager.mAdded.get(i);
                            if (FragmentManagerImpl.DEBUG) Log.v(TAG,
                                    "OP_REPLACE: adding=" + f + " old=" + old);
                            if (old.mContainerId == containerId) {
                                if (old == f) {
                                    op.fragment = f = null;
                                } else {
                                    if (op.removed == null) {
                                        op.removed = new ArrayList<Fragment>();
                                    }
                                    op.removed.add(old);
                                    old.mNextAnim = exitAnim;
                                    if (mAddToBackStack) {
                                        old.mBackStackNesting += 1;
                                        if (FragmentManagerImpl.DEBUG) Log.v(TAG, "Bump nesting of "
                                                + old + " to " + old.mBackStackNesting);
                                    }
                                    mManager.removeFragment(old, transition, transitionStyle);
                                }
                            }
                        }
                    }
                    if (f != null) {
                        f.mNextAnim = enterAnim;
                        mManager.addFragment(f, false);
                    }
                } break;
                case OP_REMOVE: {
                    Fragment f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.removeFragment(f, transition, transitionStyle);
                } break;
                case OP_HIDE: {
                    Fragment f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.hideFragment(f, transition, transitionStyle);
                } break;
                case OP_SHOW: {
                    Fragment f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.showFragment(f, transition, transitionStyle);
                } break;
                case OP_DETACH: {
                    Fragment f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.detachFragment(f, transition, transitionStyle);
                } break;
                case OP_ATTACH: {
                    Fragment f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.attachFragment(f, transition, transitionStyle);
                } break;
                default: {
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
                }
            }

            op = op.next;
        }

        mManager.moveToState(mManager.mCurState, transition, transitionStyle, true);

        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
    }
```
代码有点多，我们挑主要的分析。

代码第3~7行，提示错误是`addToBackStack() called after commit()`，那我们可以假设有这么一种情况：在某次commit中，一开始并没有调用addToBackStack()方法，所以上面代码中mAddToBackStack等于false，

```
    if (mAddToBackStack) {
        mIndex = mManager.allocBackStackIndex(this);
    } else {
        mIndex = -1;
    }
```
if条件不成立，mIndex被赋值成-1。而某菜鸟工程师保存了BackStackRecord的实例，并且手动执行了addToBackStack()方法，将mAddToBackStack的值赋值成true，所以当FragmentManagerImpl执行到本次BackStackRecord的run方法时，就碰到了上面的那个错误。

第9行，将BackStackRecord中的所有Fragment的`mBackStackNesting + 1`，对mBackStackNesting进行运算的地方有很多处，但是暂时没有找到使用mBackStackNesting值的地方，所以具体这段代码的作用目前还不清楚。

第11~21行，我暂时理解为Android5.0以上新增的在Fragment之间切换时的显示特效等等，感兴趣的朋友可以自己跟代码进去看看。

第23~24行，得到之前设置的切换风格。

第25~98行，开始遍历双向链表并依次执行所有的命令。

照例，我们还是先来看看`OP_ADD`执行的操作吧，看懂一个其他的其实都差不多。

当`op.cmd`的值为`OP_ADD`时，代码如下：

```
	Fragment f = op.fragment;
	f.mNextAnim = enterAnim;
	mManager.addFragment(f, false);
```
设置了进入动画之后，直接调用FragmentManagerImpl的addFragment方法：

```
    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        if (mAdded == null) {
            mAdded = new ArrayList<Fragment>();
        }
        if (DEBUG) Log.v(TAG, "add: " + fragment);
        makeActive(fragment);
        if (!fragment.mDetached) {
            if (mAdded.contains(fragment)) {
                throw new IllegalStateException("Fragment already added: " + fragment);
            }
            mAdded.add(fragment);
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
            }
        }
    }
```
代码第5行，将fragment添加到mAvailIndices集合中。

第6~19行，如果BackStackRecord执行过`OP_DETACH`命令，fragment.mDetached的值就会变成true，也就说已经被detach过的fragment是无法被重新add的，除非你执行`OP_ATTACH`命令。

第7~9行，检查fragment是否被重复添加。

第10~13行，添加fragment。

第14~16行，设置menu需要重绘。

第17~19行，moveToState()是一个很重要的方法，如果说前面的那些操作都只是为了保存fragment的相关信息，那么这里是真正决定fragment状态的地方。

而由于参数中的moveToStateNow为false，所以这里暂时不会执行。回头去看BackStackRecord中遍历命令链表的run()方法，你会发现其实只有在while循环中遍历完所有的操作之后，在run()方法的第100行，代码如下：

```
mManager.moveToState(mManager.mCurState, transition, transitionStyle, true);
```
方法的第一个参数是mManager.mCurState，这是个什么东西呢？搜索一下，这个值是在FragmentManagerImpl中的moveToState()方法中被赋值的，moveToState()有4个重载方法，最终都会先调用4个参数的方法，声明如下：

```
void moveToState(int newState, int transit, int transitStyle, boolean always)
```
moveToState()方法第9行有一句：

```
mCurState = newState;
```
可见，上面的mManager.mCurState的值也是来源于moveToState()的参数。那moveToState()方法又是在哪些地方被调用了呢？搜索一下，除了mCurState在初始化的时候被赋值成`int mCurState = Fragment.INITIALIZING;`以外，还可以看到大量的相似代码：

```
    public void dispatchCreate() {
        ...
        moveToState(Fragment.CREATED, false);
    }
    
    public void dispatchActivityCreated() {
        ...
        moveToState(Fragment.ACTIVITY_CREATED, false);
    }
    
    public void dispatchStart() {
        ...
        moveToState(Fragment.STARTED, false);
    }
    
    public void dispatchResume() {
        ...
        moveToState(Fragment.RESUMED, false);
    }
    
    public void dispatchPause() {
        moveToState(Fragment.STARTED, false);
    }
    
    public void dispatchStop() {
        ...
        moveToState(Fragment.STOPPED, false);
    }
    
    public void dispatchReallyStop() {
        moveToState(Fragment.ACTIVITY_CREATED, false);
    }

    public void dispatchDestroyView() {
        moveToState(Fragment.CREATED, false);
    }

    public void dispatchDestroy() {
        ...
        moveToState(Fragment.INITIALIZING, false);
        ...
    }
```
而这些dispatchXXX()方法最终都是在Activity中各种对应的生命周期方法里通过FragmentController来调用的，所以平常我们说的Fragment生命周期依赖于Activity，现在是不是一目了然了？

继续回到run方法，第100行代码如下：

```
mManager.moveToState(mManager.mCurState, transition, transitionStyle, true);
```
所以这里的mManager.mCurState应该是什么呢？

你们觉得是什么呢？其实源码看到这里，答案已经很明显了，这跟当前Fragment的生命周期也就是Activity的生命周期有关，如果运行这段代码是Activity在onStart()中，那`mManager.mCurState`的值就是`Fragment.STARTED`，如果是onCreate中，那就是`Fragment.ACTIVITY_CREATED`，与此类推...

那是不是Activity中所有的生命周期方法中都可以走到`mManager.moveToState()`这一步呢？答案显然是否定的，因为前面说过了，Fragment一旦进入了onStop()方法，就不允许再执行任何改变状态的操作了，否则会报出如下错误：

```
Caused by: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
	at android.support.v4.app.FragmentManagerImpl.checkStateLoss(FragmentManager.java:1527)
	at android.support.v4.app.FragmentManagerImpl.enqueueAction(FragmentManager.java:1545)
	at android.support.v4.app.BackStackRecord.commitInternal(BackStackRecord.java:654)
	at android.support.v4.app.BackStackRecord.commit(BackStackRecord.java:621)
```
moveToState()方法第一个参数就是这样，现在来看方法的源码：

```
    void moveToState(int newState, int transit, int transitStyle, boolean always) {
        if (mHost == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No host");
        }

        if (!always && mCurState == newState) {
            return;
        }

        mCurState = newState;
        if (mActive != null) {
            boolean loadersRunning = false;
            for (int i=0; i<mActive.size(); i++) {
                Fragment f = mActive.get(i);
                if (f != null) {
                    moveToState(f, newState, transit, transitStyle, false);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            if (!loadersRunning) {
                startPendingDeferredFragments();
            }

            if (mNeedMenuInvalidate && mHost != null && mCurState == Fragment.RESUMED) {
                mHost.onSupportInvalidateOptionsMenu();
                mNeedMenuInvalidate = false;
            }
        }
    }
```
直接从第10行开始吧，mActive保存的是当前所有被add()过的fragment，如果`mActive == null`，下面的逻辑自然而然也就没有了意义。

第11~20行，将mActive中的所有fragment都更新到最新的state。

需要注意的是，在第11行定义了一个`boolean loadersRunning = false`变量，用来记录当前fragment中是否还有尚在运行中的Loaders，如果没有，则loadersRunning的值为false，第22行`if (!loadersRunning)`条件成立，然后就会调用`startPendingDeferredFragments()`方法来开启之前被挂起的各种fragment任务；如果当前有正在运行的Loaders，则不会开始执行之前被挂起的任务：

```
    void startPendingDeferredFragments() {
        if (mActive == null) return;

        for (int i=0; i<mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f != null) {
                performPendingDeferredStart(f);
            }
        }
    }
```
遍历mActive集合中所有的Fragment，并调用performPendingDeferredStart()方法执行该fragment被挂起的任务：

```
    public void performPendingDeferredStart(Fragment f) {
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, mCurState, 0, 0, false);
        }
    }
```
如果fragment当前被挂起，即`if (f.mDeferStart)`成立，则如果当前没有正在执行任务的话，就将fragment的挂起状态重置为否，即false，同时调用moveToState()将fragment更新到最新状态。

那fragment是在什么时候会被挂起呢？`f.mDeferStart == true`的情况有两种：

- 在setUserVisibleHint()方法中，当`mState < STARTED && !isVisibleToUser == true`时

- 在moveToState()方法中，当`f.mState == Fragment.INITIALIZING && !f.mUserVisibleHint == true`时

即fragment不在活动状态且对用户不可见时，即可视为被挂起的状态。

第26~29行，通知Activity更新fragment的菜单。

上面第11~20行代码中，调用5个参数的moveToState()方法来更新fragment的状态，我们来看看具体的代码吧：

```
    void moveToState(Fragment f, int newState, int transit, int transitionStyle,
            boolean keepActive) {
        // Fragments that are not currently added will sit in the onCreate() state.
        if ((!f.mAdded || f.mDetached) && newState > Fragment.CREATED) {
            newState = Fragment.CREATED;
        }
        if (f.mRemoving && newState > f.mState) {
            // While removing a fragment, we can't change it to a higher state.
            newState = f.mState;
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (f.mDeferStart && f.mState < Fragment.STARTED && newState > Fragment.STOPPED) {
            newState = Fragment.STOPPED;
        }
        if (f.mState < newState) {
            // For fragments that are created from a layout, when restoring from
            // state we don't want to allow them to be created until they are
            // being reloaded from the layout.
            if (f.mFromLayout && !f.mInLayout) {
                return;
            }  
            if (f.mAnimatingAway != null) {
                // The fragment is currently being animated...  but!  Now we
                // want to move our state back up.  Give up on waiting for the
                // animation, move to whatever the final state should be once
                // the animation is done, and then we can proceed from there.
                f.mAnimatingAway = null;
                moveToState(f, f.mStateAfterAnimating, 0, 0, true);
            }
            switch (f.mState) {
                case Fragment.INITIALIZING:
                    if (DEBUG) Log.v(TAG, "moveto CREATED: " + f);
                    if (f.mSavedFragmentState != null) {
                        f.mSavedFragmentState.setClassLoader(mHost.getContext().getClassLoader());
                        f.mSavedViewState = f.mSavedFragmentState.getSparseParcelableArray(
                                FragmentManagerImpl.VIEW_STATE_TAG);
                        f.mTarget = getFragment(f.mSavedFragmentState,
                                FragmentManagerImpl.TARGET_STATE_TAG);
                        if (f.mTarget != null) {
                            f.mTargetRequestCode = f.mSavedFragmentState.getInt(
                                    FragmentManagerImpl.TARGET_REQUEST_CODE_STATE_TAG, 0);
                        }
                        f.mUserVisibleHint = f.mSavedFragmentState.getBoolean(
                                FragmentManagerImpl.USER_VISIBLE_HINT_TAG, true);
                        if (!f.mUserVisibleHint) {
                            f.mDeferStart = true;
                            if (newState > Fragment.STOPPED) {
                                newState = Fragment.STOPPED;
                            }
                        }
                    }
                    f.mHost = mHost;
                    f.mParentFragment = mParent;
                    f.mFragmentManager = mParent != null
                            ? mParent.mChildFragmentManager : mHost.getFragmentManagerImpl();
                    f.mCalled = false;
                    f.onAttach(mHost.getContext());
                    if (!f.mCalled) {
                        throw new SuperNotCalledException("Fragment " + f
                                + " did not call through to super.onAttach()");
                    }
                    if (f.mParentFragment == null) {
                        mHost.onAttachFragment(f);
                    } else {
                        f.mParentFragment.onAttachFragment(f);
                    }

                    if (!f.mRetaining) {
                        f.performCreate(f.mSavedFragmentState);
                    } else {
                        f.restoreChildFragmentState(f.mSavedFragmentState);
                        f.mState = Fragment.CREATED;
                    }
                    f.mRetaining = false;
                    if (f.mFromLayout) {
                        // For fragments that are part of the content view
                        // layout, we need to instantiate the view immediately
                        // and the inflater will take care of adding it.
                        f.mView = f.performCreateView(f.getLayoutInflater(
                                f.mSavedFragmentState), null, f.mSavedFragmentState);
                        if (f.mView != null) {
                            f.mInnerView = f.mView;
                            if (Build.VERSION.SDK_INT >= 11) {
                                ViewCompat.setSaveFromParentEnabled(f.mView, false);
                            } else {
                                f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                            }
                            if (f.mHidden) f.mView.setVisibility(View.GONE);
                            f.onViewCreated(f.mView, f.mSavedFragmentState);
                        } else {
                            f.mInnerView = null;
                        }
                    }
                case Fragment.CREATED:
                    if (newState > Fragment.CREATED) {
                        if (DEBUG) Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                        if (!f.mFromLayout) {
                            ViewGroup container = null;
                            if (f.mContainerId != 0) {
                                if (f.mContainerId == View.NO_ID) {
                                    throwException(new IllegalArgumentException(
                                            "Cannot create fragment "
                                                    + f
                                                    + " for a container view with no id"));
                                }
                                container = (ViewGroup) mContainer.onFindViewById(f.mContainerId);
                                if (container == null && !f.mRestored) {
                                    String resName;
                                    try {
                                        resName = f.getResources().getResourceName(f.mContainerId);
                                    } catch (NotFoundException e) {
                                        resName = "unknown";
                                    }
                                    throwException(new IllegalArgumentException(
                                            "No view found for id 0x"
                                            + Integer.toHexString(f.mContainerId) + " ("
                                            + resName
                                            + ") for fragment " + f));
                                }
                            }
                            f.mContainer = container;
                            f.mView = f.performCreateView(f.getLayoutInflater(
                                    f.mSavedFragmentState), container, f.mSavedFragmentState);
                            if (f.mView != null) {
                                f.mInnerView = f.mView;
                                if (Build.VERSION.SDK_INT >= 11) {
                                    ViewCompat.setSaveFromParentEnabled(f.mView, false);
                                } else {
                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                }
                                if (container != null) {
                                    Animation anim = loadAnimation(f, transit, true,
                                            transitionStyle);
                                    if (anim != null) {
                                        setHWLayerAnimListenerIfAlpha(f.mView, anim);
                                        f.mView.startAnimation(anim);
                                    }
                                    container.addView(f.mView);
                                }
                                if (f.mHidden) f.mView.setVisibility(View.GONE);
                                f.onViewCreated(f.mView, f.mSavedFragmentState);
                            } else {
                                f.mInnerView = null;
                            }
                        }

                        f.performActivityCreated(f.mSavedFragmentState);
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragmentState);
                        }
                        f.mSavedFragmentState = null;
                    }
                case Fragment.ACTIVITY_CREATED:
                    if (newState > Fragment.ACTIVITY_CREATED) {
                        f.mState = Fragment.STOPPED;
                    }
                case Fragment.STOPPED:
                    if (newState > Fragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "moveto STARTED: " + f);
                        f.performStart();
                    }
                case Fragment.STARTED:
                    if (newState > Fragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "moveto RESUMED: " + f);
                        f.performResume();
                        f.mSavedFragmentState = null;
                        f.mSavedViewState = null;
                    }
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case Fragment.RESUMED:
                    if (newState < Fragment.RESUMED) {
                        if (DEBUG) Log.v(TAG, "movefrom RESUMED: " + f);
                        f.performPause();
                    }
                case Fragment.STARTED:
                    if (newState < Fragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "movefrom STARTED: " + f);
                        f.performStop();
                    }
                case Fragment.STOPPED:
                    if (newState < Fragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "movefrom STOPPED: " + f);
                        f.performReallyStop();
                    }
                case Fragment.ACTIVITY_CREATED:
                    if (newState < Fragment.ACTIVITY_CREATED) {
                        if (DEBUG) Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        if (f.mView != null) {
                            // Need to save the current view state if not
                            // done already.
                            if (mHost.onShouldSaveFragmentState(f) && f.mSavedViewState == null) {
                                saveFragmentViewState(f);
                            }
                        }
                        f.performDestroyView();
                        if (f.mView != null && f.mContainer != null) {
                            Animation anim = null;
                            if (mCurState > Fragment.INITIALIZING && !mDestroyed) {
                                anim = loadAnimation(f, transit, false,
                                        transitionStyle);
                            }
                            if (anim != null) {
                                final Fragment fragment = f;
                                f.mAnimatingAway = f.mView;
                                f.mStateAfterAnimating = newState;
                                final View viewToAnimate = f.mView;
                                anim.setAnimationListener(new AnimateOnHWLayerIfNeededListener(
                                        viewToAnimate, anim) {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        super.onAnimationEnd(animation);
                                        if (fragment.mAnimatingAway != null) {
                                            fragment.mAnimatingAway = null;
                                            moveToState(fragment, fragment.mStateAfterAnimating,
                                                    0, 0, false);
                                        }
                                    }
                                });
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                case Fragment.CREATED:
                    if (newState < Fragment.CREATED) {
                        if (mDestroyed) {
                            if (f.mAnimatingAway != null) {
                                // The fragment's containing activity is
                                // being destroyed, but this fragment is
                                // currently animating away.  Stop the
                                // animation right now -- it is not needed,
                                // and we can't wait any more on destroying
                                // the fragment.
                                View v = f.mAnimatingAway;
                                f.mAnimatingAway = null;
                                v.clearAnimation();
                            }
                        }
                        if (f.mAnimatingAway != null) {
                            // We are waiting for the fragment's view to finish
                            // animating away.  Just make a note of the state
                            // the fragment now should move to once the animation
                            // is done.
                            f.mStateAfterAnimating = newState;
                            newState = Fragment.CREATED;
                        } else {
                            if (DEBUG) Log.v(TAG, "movefrom CREATED: " + f);
                            if (!f.mRetaining) {
                                f.performDestroy();
                            } else {
                                f.mState = Fragment.INITIALIZING;
                            }

                            f.performDetach();
                            if (!keepActive) {
                                if (!f.mRetaining) {
                                    makeInactive(f);
                                } else {
                                    f.mHost = null;
                                    f.mParentFragment = null;
                                    f.mFragmentManager = null;
                                }
                            }
                        }
                    }
            }
        }

        if (f.mState != newState) {
            Log.w(TAG, "moveToState: Fragment state for " + f + " not updated inline; "
                    + "expected state " + newState + " found " + f.mState);
            f.mState = newState;
        }
    }
```
首先来看看Fragment的各种状态的值：

```
    static final int INITIALIZING = 0;     // Not yet created.
    static final int CREATED = 1;          // Created.
    static final int ACTIVITY_CREATED = 2; // The activity has finished its creation.
    static final int STOPPED = 3;          // Fully created, not started.
    static final int STARTED = 4;          // Created and started, not resumed.
    static final int RESUMED = 5;          // Created started and resumed.
```
第1~4行，未被add()的fragment将被置成`Fragment.CREATED`状态。

第5~8行，当fragment被remove时，f.mState的值是`Fragment.INITIALIZING`或者`Fragment.CREATED`，如果此时`newState > f.mState`，则newState的值被重置成`f.mState`的值。

第9~13行，如果需要挂起本次操作，如果fragment尚未start的话，则不允许它的状态变成`STARTED`或者更高。

第14~271行，有两种情况，精简后的代码如下：

```
        if (f.mState < newState) {
            switch (f.mState) {
                case Fragment.INITIALIZING:
                    
                case Fragment.CREATED:
                    
                case Fragment.ACTIVITY_CREATED:
                    
                case Fragment.STOPPED:
                    
                case Fragment.STARTED:
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case Fragment.RESUMED:
                    
                case Fragment.STARTED:
                    
                case Fragment.STOPPED:
                    
                case Fragment.ACTIVITY_CREATED:
                    
                case Fragment.CREATED:    
            }
        }
```
恩~看着苏胡多了~~

注意，这里switch-case中都没有break，并不是我手动去掉了哦。所以说如果只要f.mState的值满足了其中的一个case，那么该case以及后面的所有case都会被走到。这点应该不难理解吧？

继续分析。

如果当前状态比目标状态要低，则依次“提升”fragment的等级，并依次调用Fragment相关的方法：

| f.mState 旧值          | Fragment 方法                                                                                 |
| --------------------- | :-------------------------------------------------------------------------------------------: |
| Fragment.INITIALIZING | onAttach()、performCreate()、restoreChildFragmentState()、performCreateView()、onViewCreated() |
| Fragment.CREATED      | performCreateView()、onViewCreated()、performActivityCreated()、restoreViewState()             |
| Fragment.STOPPED      | performStart()                                                                                |
| Fragment.STARTED      | performResume()                                                                               |

如果当前状态比目标状态要高，则依次“降低”fragment的等级，同样，也会依次调用fragment相关的各种方法：

| f.mState                  | Fragment 方法                                                                                 |
| ------------------------  | :-------------------------------------------------------------------------------------------: |
| Fragment.RESUMED          | performPause()                                                                                |
| Fragment.STARTED          | performStop()                                                                                 |
| Fragment.STOPPED          | performReallyStop()                                                                           |
| Fragment.ACTIVITY_CREATED | performDestroyView()                                                                          |
| Fragment.CREATED          | performDestroy()、performDetach()                                                             |

然后fragment生命周期的各种onXXX()方法都是在performXXX()中执行，具体细节就不跟了，否则这文章写到明年也写不完了。

当然除了fragment自己的生命周期方法会被调用以外，在各种case中也可能对ParentFragment、ChildFragments的生命周期方法进行调用，比如在第一个if的case Fragment.INITIALIZING中有如下代码：

```
   if (f.mParentFragment == null) {
   		//fragment添加在Activity中
       mHost.onAttachFragment(f);
   } else {
   		//fragment嵌套在fragment中
       f.mParentFragment.onAttachFragment(f);
   }
   
   if (!f.mRetaining) {
       f.performCreate(f.mSavedFragmentState);
   } else {
   		//保存child fragment的状态
       f.restoreChildFragmentState(f.mSavedFragmentState);
       f.mState = Fragment.CREATED;
   }
```
至此，Fragment的源码暂时就分析到这里了，欢迎大家交流，如果需要补充我有空时会再更新的。

转载请注明出处，谢谢！

####[Android源码阅读笔记(1)----Fragment]()