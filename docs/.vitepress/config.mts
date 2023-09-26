import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    title: "Gsvc Doc",
    description: "A Toy Based on Spring Boot & grpc-java",
    base: "/",
    themeConfig: {
        search: {
            provider: 'local'
        },
        nav: [
            {text: 'Home', link: '/'},
            //{text: 'Examples', link: '/markdown-examples'}
        ],

        sidebar: [
            {text: '立即开始', link: '/guide/get-start'}
        ],

        socialLinks: [
            {icon: 'github', link: 'https://github.com/ninggf/gsvc'}
        ]
    }
})
