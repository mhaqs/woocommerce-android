package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductFilterListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImageViewerFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImagesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductInventoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductPricingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingClassFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVariantsFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ProductDetailFragmentModule::class,
    ProductListFragmentModule::class,
    ProductFilterListFragmentModule::class,
    ProductVariantsFragmentModule::class,
    ProductImagesFragmentModule::class,
    ProductImageViewerFragmentModule::class,
    ProductInventoryFragmentModule::class,
    ProductShippingFragmentModule::class,
    ProductShippingClassFragmentModule::class,
    ProductPricingFragmentModule::class
])
object ProductsModule {
    @Module
    abstract class ProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductListModule::class])
        abstract fun productListFragment(): ProductListFragment
    }

    @Module
    abstract class ProductFilterListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductFilterListModule::class])
        abstract fun productFilterListFragment(): ProductFilterListFragment
    }

    @Module
    abstract class ProductDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailModule::class])
        abstract fun productDetailFragment(): ProductDetailFragment
    }

    @Module
    internal abstract class ProductVariantsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductVariantsModule::class])
        abstract fun productVariantsFragment(): ProductVariantsFragment
    }

    @Module
    internal abstract class ProductInventoryFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductInventoryModule::class])
        abstract fun productInventoryFragment(): ProductInventoryFragment
    }

    @Module
    internal abstract class ProductShippingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingModule::class])
        abstract fun productShippingFragment(): ProductShippingFragment
    }

    @Module
    internal abstract class ProductShippingClassFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingClassModule::class])
        abstract fun productShippingClassFragment(): ProductShippingClassFragment
    }

    @Module
    internal abstract class ProductImagesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImagesModule::class])
        abstract fun productImagesFragment(): ProductImagesFragment
    }

    @Module
    internal abstract class ProductImageViewerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImageViewerModule::class])
        abstract fun productImageViewerFragment(): ProductImageViewerFragment
    }

    @Module
    internal abstract class ProductPricingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductPricingModule::class])
        abstract fun productPricingFragment(): ProductPricingFragment
    }
}
