package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductImagesViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

class ProductDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private const val OFFLINE_PRODUCT_REMOTE_ID = 2L
    }

    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val productCategoriesRepository: ProductCategoriesRepository = mock()
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on(it.formatCurrency(any<BigDecimal>(), any(), any())).thenAnswer { i -> "${i.arguments[1]}${i.arguments[0]}" }
    }
    private val savedState: SavedStateWithArgs = mock()

    private val coroutineDispatchers = CoroutineDispatchers(
            Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val product = ProductTestUtils.generateProduct(PRODUCT_REMOTE_ID)
    private val offlineProduct = ProductTestUtils.generateProduct(OFFLINE_PRODUCT_REMOTE_ID)
    private lateinit var viewModel: ProductDetailViewModel

    private val productWithParameters = ProductDetailViewState(
            productDraft = product,
            storedProduct = product,
            isSkeletonShown = false,
            uploadingImageUris = null,
            weightWithUnits = "10kg",
            sizeWithUnits = "1 x 2 x 3 cm",
            salePriceWithCurrency = "CZK10.00",
            regularPriceWithCurrency = "CZK30.00"
    )

    @Before
    fun setup() {
        doReturn(MutableLiveData(ProductDetailViewState()))
                .whenever(savedState).getLiveData<ProductDetailViewState>(any(), any())
        doReturn(MutableLiveData(ProductImagesViewState()))
                .whenever(savedState).getLiveData<ProductImagesViewState>(any(), any())

        viewModel = spy(
                ProductDetailViewModel(
                        savedState,
                        coroutineDispatchers,
                        selectedSite,
                        productRepository,
                        productCategoriesRepository,
                        networkStatus,
                        currencyFormatter,
                        wooCommerceStore,
                        productImagesServiceWrapper
                )
        )
        val prodSettings = WCProductSettingsModel(0).apply {
            dimensionUnit = "cm"
            weightUnit = "kg"
        }
        val siteSettings = mock<WCSettingsModel> {
            on(it.currencyCode).thenReturn("CZK")
        }

        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(prodSettings).whenever(wooCommerceStore).getProductSettings(any())
        doReturn(siteSettings).whenever(wooCommerceStore).getSiteSettings(any())
    }

    @Test
    fun `Displays the product detail view correctly`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        assertThat(productData).isEqualTo(ProductDetailViewState())

        viewModel.start(PRODUCT_REMOTE_ID)

        assertThat(productData).isEqualTo(productWithParameters)
    }

    @Test
    fun `Display error message on fetch product error`() = test {
        whenever(productRepository.fetchProduct(PRODUCT_REMOTE_ID)).thenReturn(null)
        whenever(productRepository.getProduct(PRODUCT_REMOTE_ID)).thenReturn(null)

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        verify(productRepository, times(1)).fetchProduct(PRODUCT_REMOTE_ID)

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.product_detail_fetch_product_error))
    }

    @Test
    fun `Do not fetch product from api when not connected`() = test {
        doReturn(offlineProduct).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        verify(productRepository, times(1)).getProduct(PRODUCT_REMOTE_ID)
        verify(productRepository, times(0)).fetchProduct(any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows and hides product detail skeleton correctly`() = test {
        doReturn(null).whenever(productRepository).getProduct(any())

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.productDetailViewStateData.observeForever {
            old, new -> new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Displays the updated product detail view correctly`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        assertThat(productData).isEqualTo(ProductDetailViewState())

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData).isEqualTo(productWithParameters)

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.productDraft?.description).isEqualTo(updatedDescription)
    }

    @Test
    fun `Displays update menu action if product is edited`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.isProductUpdated).isNull()

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.isProductUpdated).isTrue()
    }

    @Test
    fun `Displays progress dialog when product is edited`() = test {
        doReturn(product).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(productRepository).updateProduct(any())

        val isProgressDialogShown = ArrayList<Boolean>()
        viewModel.productDetailViewStateData.observeForever { old, new ->
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                isProgressDialogShown.add(it)
            } }

        viewModel.start(PRODUCT_REMOTE_ID)
        viewModel.onUpdateButtonClicked()

        assertThat(isProgressDialogShown).containsExactly(true, false)
    }

    @Test
    fun `Do not update product when not connected`() = test {
        doReturn(product).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start(PRODUCT_REMOTE_ID)
        viewModel.onUpdateButtonClicked()

        verify(productRepository, times(0)).updateProduct(any())
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
        assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display error message on update product error`() = test {
        doReturn(product).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(productRepository).updateProduct(any())

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start(PRODUCT_REMOTE_ID)
        viewModel.onUpdateButtonClicked()

        verify(productRepository, times(1)).updateProduct(any())
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.product_detail_update_product_error))
        assertThat(productData?.isProgressDialogShown).isFalse()
    }

    @Test
    fun `Display success message on update product success`() = test {
        doReturn(product).whenever(productRepository).getProduct(any())
        doReturn(true).whenever(productRepository).updateProduct(any())

        var successSnackbarShown = false
        viewModel.event.observeForever {
            if (it is ShowSnackbar && it.message == R.string.product_detail_update_product_success) {
                successSnackbarShown = true
            }
        }

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start(PRODUCT_REMOTE_ID)
        viewModel.onUpdateButtonClicked()

        verify(productRepository, times(1)).updateProduct(any())
        verify(productRepository, times(2)).getProduct(PRODUCT_REMOTE_ID)

        assertThat(successSnackbarShown).isTrue()
        assertThat(productData?.isProgressDialogShown).isFalse()
        assertThat(productData?.isProductUpdated).isFalse()
        assertThat(productData?.productDraft).isEqualTo(product)
    }

    @Test
    fun `Correctly sorts the Product Categories By their Parent Ids and by name`() = test {
        val list = generateTestProductCategories()
        val sortedByNameAndParent = viewModel.sortCategoriesByNameAndParent(list).toList()
        assertThat(sortedByNameAndParent[0].category).isEqualTo(list[0])
        assertThat(sortedByNameAndParent[1].category).isEqualTo(list[7])
        assertThat(sortedByNameAndParent[2].category).isEqualTo(list[10])
        assertThat(sortedByNameAndParent[3].category).isEqualTo(list[1])
        assertThat(sortedByNameAndParent[4].category).isEqualTo(list[6])
        assertThat(sortedByNameAndParent[5].category).isEqualTo(list[8])
        assertThat(sortedByNameAndParent[6].category).isEqualTo(list[9])
        assertThat(sortedByNameAndParent[7].category).isEqualTo(list[2])
        assertThat(sortedByNameAndParent[8].category).isEqualTo(list[3])
        assertThat(sortedByNameAndParent[9].category).isEqualTo(list[5])
        assertThat(sortedByNameAndParent[10].category).isEqualTo(list[4])
    }

    @Test
    fun `Correctly computes the cascading margin for the product Category by their Parent Ids`() = test {
        val list = generateTestProductCategories()
        val sortedAndStyledList = viewModel.sortAndStyleProductCategories(product, list)

        assertThat(sortedAndStyledList[0].category).isEqualTo(list[0])
        assertThat(sortedAndStyledList[1].category).isEqualTo(list[7])
        assertThat(sortedAndStyledList[2].category).isEqualTo(list[10])

        assertThat(sortedAndStyledList[7].margin).isEqualTo(32)
        assertThat(sortedAndStyledList[8].margin).isEqualTo(64)
        assertThat(sortedAndStyledList[9].margin).isEqualTo(96)
    }

    private fun generateTestProductCategories(): List<ProductCategory> {
        val list = mutableListOf<ProductCategory>()
        list.add(ProductCategory(1, "A", "a", 0))
        list.add(ProductCategory(2, "B", "b", 0))
        list.add(ProductCategory(3, "C", "c", 0))
        list.add(ProductCategory(4, "CA", "ca", 3))
        list.add(ProductCategory(5, "CAA", "caa", 3))
        list.add(ProductCategory(6, "CACA", "caca", 4))
        list.add(ProductCategory(7, "BA", "ba", 2))
        list.add(ProductCategory(8, "b", "b1", 0))
        list.add(ProductCategory(9, "c", "c1", 0))
        list.add(ProductCategory(10, "ca", "ca1", 9))
        list.add(ProductCategory(11, "ba", "ba1", 8))
        return list
    }
}
