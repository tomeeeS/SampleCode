/* This is a fragment from one of my apps, and some of its dependencies.
 I believe it shows my knowledge and usage of: 
    - clean code
    - SOLID principles
    - sw architectures (currently using MVVM(I))
    - some (relatively) new and powerful Kotlin & Android features (from Jetpack & Architecture components, Kotlin coroutines)

 PlaceOrderFragment shows a pdf on the UI, and with a button click it can open an
  email client app with an intent, and have the pdf automatically attached and some
  information like the client's email address prefilled */

class PlaceOrderFragment: ViewPdfFragment<FragmentPlaceOrderBinding>(R.layout.fragment_place_order) {

    override val viewModel by viewModels<PlaceOrderViewModel>() 

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
    	initButtons()
        return binding.root
    }

    private fun initButtons() {
        with(binding) { // (in a parent class: binding = DataBindingUtil.inflate(inflater, layoutResourceId, container, false))
            okButton.setOnClickListener {
                val clientEmail = clientRepo.get(currentOrder.clientId).email
                emailOrder(viewModel.contentUri, clientEmail)
            }
            cancelButton.setOnClickListener { activity!!.onBackPressed() }
        }
    }

    override fun onPdfGenerated() { 
        binding.pdf.fromFile(viewModel.orderPdf)  // binding.pdf: third party pdf viewer 
            .onLoad { binding.okButton.isEnabled = true }
            .spacing(pageSpacing) 
            .load()
    }

    private fun emailOrder(orderPdfUri: Uri, clientEmail: String) {
        sendEmail(get<EmailManager>().createEmailIntent(orderPdfUri, clientEmail)) // get<>(): gets object via service locator. 
            // gets my implementation of my EmailManager interface
    }

    private fun sendEmail(emailIntent: Intent) {
        startActivity(
            Intent.createChooser(
                emailIntent,
                getString(R.string.send_email)
            )
        )
    }
} // (by viewModels<>(): fragments ktx extension function. helps to avoid having to write factories for VMs)


// these are some of the stuff PlaceOrderFragment depends on:

// removes the duplication of common pdf viewer fragment code, (I have an other fragment that shows a pdf too)
abstract class ViewPdfFragment<ViewDataBindingType: ViewDataBinding>
    (layoutResourceId: Int)
    : DataBindingFragment<ViewDataBindingType>(layoutResourceId) {

    abstract val viewModel: PdfViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        loadPdf()
        return binding.root
    }

    private fun loadPdf() {
        ioWork(
            viewLifecycleOwner,
            viewModel::generatePdf,
            ::onPdfGenerated
        )
    }

    abstract fun onPdfGenerated()

    companion object {
        const val pageSpacing = 8
    }
}

// coroutine helper to do some "work" on an IO-optimized thread, then if need be, perform some UI work (on the main thread)
fun ioWork(lifecycleOwner: LifecycleOwner?, work: () -> Unit, useUiOnComplete: () -> Unit = {}) {
    (lifecycleOwner?.lifecycleScope ?: GlobalScope).launch(Dispatchers.IO) {
        work()
        withContext(Dispatchers.Main) { useUiOnComplete() }
    }
}

// common parent fragment to remove duplications concerning creating the data binding for fragments
open class DataBindingFragment<ViewDataBindingType: ViewDataBinding>
    (private val layoutResourceId: Int)
    : Fragment(){

    protected lateinit var binding: ViewDataBindingType

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, layoutResourceId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

}

abstract class PdfViewModel : ViewModel(), KoinComponent {
    abstract fun generatePdf()
}

class PlaceOrderViewModel: PdfViewModel() {

    lateinit var orderPdf: File
    lateinit var contentUri: Uri

    override fun generatePdf() {
        orderPdf = ManageOrdersInteractor.generateOrderPdf()
        contentUri = MyFileProvider.getOrderPdfUri(orderPdf)
    }

}

// fragment_place_order.xml:
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        >

        <com.github.barteksc.pdfviewer.PDFView
            android:id="@+id/pdf"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:background="@color/background"

            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toTopOf="@+id/cancel_button"
            >
        </com.github.barteksc.pdfviewer.PDFView>

        <hu.sajti.pevdicalculator.view.SquareImageButton
            android:id="@+id/cancel_button"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_gravity="right"
            android:background="@drawable/red_cancel"
            android:scaleType="fitXY"
            android:layout_margin="8dp"

            app:layout_constraintWidth_percent="@dimen/button_width_ratio"

            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            />

        <hu.sajti.pevdicalculator.view.SquareImageButton
            android:id="@+id/ok_button"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_gravity="right"
            android:background="@drawable/arrow_blue_right"
            android:scaleType="fitXY"
            android:layout_margin="8dp"
            isEnabled="false"

            app:layout_constraintWidth_percent="@dimen/button_width_ratio"

            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            />

    </androidx.constraintlayout.widget.ConstraintLayout>

</layout>
